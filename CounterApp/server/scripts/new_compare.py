import os
import torch
import numpy as np
from tqdm import tqdm
from torchvision.datasets import ImageFolder
from torch.utils.data import DataLoader
import datetime
import sqlite3
from build_db import ImageFeatureExtractor

def log_to_file(log_file, message, print_to_console=True):
    """Log messages to both console and file."""
    if print_to_console:
        print(message)
    if log_file:
        with open(log_file, 'a', encoding='utf-8') as f:
            f.write(message + '\n')

def load_prototypes_from_database(db_file):
    """Load prototype features from SQLite database."""
    if not os.path.exists(db_file):
        print(f"Error: Feature database {db_file} does not exist.")
        return {}

    try:
        conn = sqlite3.connect(db_file)
        cursor = conn.cursor()

        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='prototypes'")
        if not cursor.fetchone():
            print("Warning: No prototype table in the database.")
            conn.close()
            return {}

        cursor.execute("SELECT label, feature, sample_count FROM prototypes")
        rows = cursor.fetchall()

        prototypes = {
            label: {
                'feature': np.frombuffer(feature_bytes, dtype=np.float32),
                'sample_count': sample_count
            }
            for label, feature_bytes, sample_count in rows
        }

        conn.close()
        print(f"Loaded {len(prototypes)} prototype categories from {db_file}.")
        return prototypes

    except Exception as e:
        print(f"Error reading prototypes from database: {e}")
        return {}

def cosine_similarity(vec1, vec2):
    """Calculate cosine similarity between two vectors."""
    return np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2))

def predict_and_display(test_dir, db_file='train_features.db', log_file=None, selected_label=None, threshold=0.75):
    """Predict using prototype features and display results."""
    extractor = ImageFeatureExtractor(model_path=model_path)
    prototypes = load_prototypes_from_database(db_file)

    if not prototypes:
        log_to_file(log_file, "Error: Unable to load prototypes.")
        return

    log_to_file(log_file, f"Loaded {len(prototypes)} prototype categories.")

    if selected_label and selected_label in prototypes:
        prototypes = {selected_label: prototypes[selected_label]}
        log_to_file(log_file, f"Using prototype for label '{selected_label}'.")

    test_transform = extractor.transform
    test_dataset = ImageFolder(root=test_dir, transform=test_transform)
    test_loader = DataLoader(test_dataset, batch_size=1, shuffle=False)

    results = []
    for idx, (img, class_idx) in enumerate(tqdm(test_loader, desc="Processing images")):
        img_path = test_dataset.samples[idx][0]
        img_name = os.path.basename(img_path)
        class_name = test_dataset.classes[class_idx.item()]

        process_test_image_with_prototype(
            img_path, img_name, prototypes, extractor, results, threshold, log_file, class_name
        )

    display_results_summary(results, log_file)
    return results

def process_test_image_with_prototype(test_img_path, test_img_name, prototypes, extractor, results, threshold, log_file=None, class_name=None):
    """Process a single test image using prototypes."""
    query_feature = extractor.extract_features(test_img_path)
    if query_feature is None:
        return

    prototype_similarities = [
        (label, cosine_similarity(query_feature, prototype_data['feature']), prototype_data['sample_count'])
        for label, prototype_data in prototypes.items()
    ]

    prototype_similarities.sort(key=lambda x: x[1], reverse=True)
    if not prototype_similarities:
        return

    most_similar_label, highest_similarity, sample_count = prototype_similarities[0]
    decision = "Accept" if highest_similarity >= threshold else "Reject"
    final_label = most_similar_label if decision == "Accept" else "??"

    results.append({
        'test_image': test_img_name,
        'class_name': class_name,
        'most_similar_label': most_similar_label,
        'similarity': highest_similarity,
        'sample_count': sample_count,
        'decision': decision,
        'final_label': final_label
    })

def display_results_summary(results, log_file=None):
    """Display summary of results."""
    header = f"{'Image':<25} | {'Label':<10} | {'Similar':<7} | {'Decision':<10}"
    separator = "-" * len(header)

    log_to_file(log_file, "Prediction Results Summary:")
    log_to_file(log_file, separator)
    log_to_file(log_file, header)
    log_to_file(log_file, separator)

    accepted = sum(1 for result in results if "Accept" in result['decision'])
    rejected = len(results) - accepted

    for result in results:
        sim_str = f"{result['similarity']:.4f}"
        line = f"{result['test_image']:<25} | {result['final_label']:<10} | {sim_str:<7} | {result['decision']:<10}"
        log_to_file(log_file, line)

    log_to_file(log_file, separator)
    log_to_file(log_file, f"Total Processed: {len(results)} images")
    log_to_file(log_file, f"Accepted: {accepted} ({accepted/len(results)*100:.1f}%)")
    log_to_file(log_file, f"Rejected: {rejected} ({rejected/len(results)*100:.1f}%)")

if __name__ == "__main__":
    test_dir = 'feature_db/test/'
    model_path = 'output/simclr_mobilenetv3.pth'
    db_file = 'output/train_features.db'
    selected_label = None
    threshold = 0.7

    result_dir = 'output/result/'
    os.makedirs(result_dir, exist_ok=True)

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    label_info = f"_{selected_label}" if selected_label else ""
    log_file = f'{result_dir}prediction_prototype{label_info}_{timestamp}.txt'

    print(f"\nStarting prediction...\nResults will be saved to: {log_file}")
    predict_and_display(test_dir, db_file, log_file, selected_label, threshold)
