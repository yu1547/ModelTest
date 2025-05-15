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
    # print(message)

# 載入資料庫中的特徵 (正確的)
def load_features_from_database(db_file):
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    cursor.execute("SELECT file_path, label, feature FROM features")
    rows = cursor.fetchall()

    file_paths = []
    labels = []
    features = []

    for file_path, label, feature_bytes in rows:
        # 將二進制數據轉換回numpy數組
        feature = np.frombuffer(feature_bytes, dtype=np.float32)

        file_paths.append(file_path)
        labels.append(label)
        features.append(feature)

    conn.close()
    return features, file_paths, labels

# 結合了old_compare.py中的兩個函數的功能
def predict_from_feature_vector(feature_vector, db_file, log_file=None, test_img_name="未知圖片", class_name=None):
    # 記錄開始時間
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    log_to_file(log_file, f"\n========== 特徵向量比對開始 {timestamp} ==========\n")

    # 載入資料庫特徵
    train_features, train_file_paths, train_labels = load_features_from_database(db_file)
    if len(train_features) == 0:
        log_to_file(log_file, "訓練資料特徵為空，無法進行比對")
        return []

    # 計算所有相似度
    similarities = []
    for idx, train_feature in enumerate(train_features):
        similarity = np.dot(feature_vector, train_feature) / (
            np.linalg.norm(feature_vector) * np.linalg.norm(train_feature)
        )
        similarities.append((similarity, train_labels[idx]))

    # 排序（修正排序邏輯）
    similarities.sort(key=lambda x: x[0], reverse=True)  # 注意這邊是 x[0] 而不是 x[1]

    # 選擇第一個最相似的結果
    similar_images = similarities[:1]
    highest_similarity, most_similar_class = similar_images[0]

    # 雙重門檻判定
    prediction_confidence = "未知"
    prediction_reason = ""

    if highest_similarity > 0.8:
        # 高可信度 - 直接採用最相似圖片的類別
        prediction_confidence = "高可信度"
        prediction_reason = f"相似度 {highest_similarity:.4f} > 0.8"
    elif highest_similarity < 0.7:
        # 低可信度 - 判定為未知類別
        most_similar_class = "未知類別"
        prediction_confidence = "低可信度"
        prediction_reason = f"相似度 {highest_similarity:.4f} < 0.7"
    else:
        # 中等可信度 - 按類別分組，比較不同類別間的相似度差距
        class_best = {}
        # 將相似圖像按類別分組，每個類別只保留最高相似度
        for sim, label in similarities:
            if label not in class_best or sim > class_best[label][1]:
                class_best[label] = (label, sim)

        # 按相似度排序類別
        sorted_classes = sorted([(label, sim) for label, (_, sim) in class_best.items()],
                                key=lambda x: x[1], reverse=True)

        # 如果只有一個類別，則採用該類別
        if len(sorted_classes) == 1:
            best_class, best_sim = sorted_classes[0]
            most_similar_class = best_class
            prediction_confidence = "中可信度-採用"
            prediction_reason = "僅有一個匹配類別"
        else:
            # 獲取最高相似度的類別 (A) 和次高相似度的類別 (B)
            best_class, best_sim = sorted_classes[0]
            second_best_class, second_best_sim = sorted_classes[1]
            similarity_gap = best_sim - second_best_sim

            # 若最佳與次佳類別相似度差距大於0.1，採用最佳結果
            if similarity_gap > 0.1:
                most_similar_class = best_class
                prediction_confidence = "中可信度-採用"
                prediction_reason = f"類別間相似度差距 {similarity_gap:.4f} > 0.1 (最佳:{best_class}={best_sim:.4f}, 次佳:{second_best_class}={second_best_sim:.4f})"
            else:
                most_similar_class = "未知類別"
                prediction_confidence = "中可信度-拒絕"
                prediction_reason = f"類別間相似度差距 {similarity_gap:.4f} <= 0.1 (最佳:{best_class}={best_sim:.4f}, 次佳:{second_best_class}={second_best_sim:.4f})"

    # 建立結果
    result = {
        # 'test_image': test_img_name,
        # 'class_name': class_name,
        'most_similar_class': most_similar_class,
        'similarity': float(highest_similarity),
        'confidence': prediction_confidence,
        'reason': prediction_reason
    }
    print(json.dumps(result))
    
    # log 輸出
    class_info = f" (類別: {class_name})" if class_name else ""
    # log_to_file(log_file, f"輸入圖片：{test_img_name}{class_info}")
    log_to_file(log_file, f"判斷類別: {most_similar_class} ") # 如果你只要輸出類別的話把其他的註解掉就好囉
    log_to_file(log_file, f"相似度: {highest_similarity:.4f}, {prediction_confidence}")
    log_to_file(log_file, f"判斷依據: {prediction_reason}")
    log_to_file(log_file, f"========== 比對結束 {timestamp} ==========\n")

    return result


if __name__ == "__main__":
    
    # 從命令列獲取特徵向量
    try:
        feature_vector = np.array(json.loads(sys.argv[1]), dtype=np.float32)
    except Exception as e:
        print(f"錯誤：解析特徵向量失敗 - {e}", file=sys.stderr)
        sys.exit(1)

    # './train_features.db'得在該資料夾中執行才有效
    db_file = './train_features.db'

    # result_dir = './output'
    # os.makedirs(result_dir, exist_ok=True)

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    # log_file = os.path.join(result_dir, f'predict_from_vector_{timestamp}.txt')

    # print(f"\n執行特徵向量比對...\n結果將保存到: {log_file}")
    # print(f"\n執行特徵向量比對...: ")
    predict_from_feature_vector(feature_vector, db_file, log_file=None)


