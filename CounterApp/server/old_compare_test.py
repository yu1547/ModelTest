import os
import datetime
import numpy as np
import sqlite3
import sys
import json
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')


def log_to_file(log_file, message):
    if log_file:
        with open(log_file, 'a') as f:
            f.write(message + '\n')
    print(message)

def load_features_from_database(db_file):
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    cursor.execute("SELECT file_path, label, feature FROM features")

    file_paths = []
    labels = []
    features = []

    for row in cursor.fetchall():
        path, label, feature_blob = row
        feature_array = np.frombuffer(feature_blob, dtype=np.float32)
        file_paths.append(path)
        labels.append(label)
        features.append(feature_array)

    conn.close()
    return features, file_paths, labels

def predict_from_feature_vector(feature_vector, top_k=5, db_file='train_features.db', log_file=None):
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    log_to_file(log_file, f"\n========== 特徵向量比對開始 {timestamp} ==========\n")

    train_features, train_file_paths, train_labels = load_features_from_database(db_file)
    if len(train_features) == 0:
        log_to_file(log_file, "訓練資料特徵為空，無法進行比對")
        return []

    similarities = []
    for idx, train_feature in enumerate(train_features):
        similarity = np.dot(feature_vector, train_feature) / (
            np.linalg.norm(feature_vector) * np.linalg.norm(train_feature)
        )
        similarities.append((similarity, train_file_paths[idx], train_labels[idx]))

    similarities.sort(reverse=True, key=lambda x: x[0])
    top_matches = similarities[:top_k]

    for rank, (sim, path, label) in enumerate(top_matches, start=1):
        log_to_file(log_file, f"Top {rank}: 類別={label}, 相似度={sim:.4f}, 圖片={path}")

    log_to_file(log_file, f"\n========== 比對結束 {timestamp} ==========\n")
    return top_matches

if __name__ == "__main__":

    # 從命令列獲取特徵向量
    try:
        feature_vector = np.array(json.loads(sys.argv[1]), dtype=np.float32)
    except Exception as e:
        print(f"錯誤：解析特徵向量失敗 - {e}", file=sys.stderr)
        sys.exit(1)
        
    db_file = './train_features.db'
    # result_dir = './output'
    # os.makedirs(result_dir, exist_ok=True)

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    # log_file = os.path.join(result_dir, f'predict_from_vector_{timestamp}.txt')

    top_k = 5
    # print(f"\n執行特徵向量比對...\n結果將保存到: {log_file}")
    print(f"\n執行特徵向量比對...: ")
    predict_from_feature_vector(feature_vector, top_k, db_file, log_file=None)
