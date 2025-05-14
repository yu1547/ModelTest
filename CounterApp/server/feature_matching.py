import sys
import json
import numpy as np
import sqlite3
import os

# 讀取 Express.js 傳入的特徵向量
if len(sys.argv) > 1:
    try:
        feature_vector = np.array(json.loads(sys.argv[1]))
    except json.JSONDecodeError as e:
        # print(f"❌ JSON 解析失敗: {e}")
        sys.exit(1)
else:
    # print("❌ 沒有收到特徵向量")
    sys.exit(1)

# 加載 SQLite 資料庫中的原型特徵
def load_prototypes_from_database(db_file):
    """從 SQLite 數據庫讀取原型特徵數據"""
    if not os.path.exists(db_file):
        # print(f"❌ 錯誤：特徵數據庫 {db_file} 不存在")
        return {}
    
    try:
        conn = sqlite3.connect(db_file)
        cursor = conn.cursor()
        
        # 檢查原型表是否存在
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='prototypes'")
        if not cursor.fetchone():
            # print("⚠️ 警告：數據庫中沒有原型表")
            conn.close()
            return {}
        
        # 讀取所有原型
        cursor.execute("SELECT label, feature, sample_count FROM prototypes")
        rows = cursor.fetchall()
        
        prototypes = {}
        for label, feature_bytes, sample_count in rows:
            # 將二進制數據轉換回 numpy 數組
            # print(f"🔍 {label} 的 feature_bytes 類型: {type(feature_bytes)}, 長度: {len(feature_bytes)}")
            feature = np.frombuffer(feature_bytes, dtype=np.float32)
            prototypes[label] = {
                'feature': feature,
                'sample_count': sample_count
            }
            
        conn.close()
        # print(f"✅ 已從 {db_file} 中載入 {len(prototypes)} 個類別的原型特徵")
        return prototypes
    except Exception as e:
        # print(f"❌ 從數據庫讀取原型時發生錯誤: {e}")
        return {}

# 計算餘弦相似度
def cosine_similarity(vec1, vec2):
    if vec1.shape[0] != vec2.shape[0]:
        # print(f"⚠️ 維度錯誤：vec1({vec1.shape[0]}) vs vec2({vec2.shape[0]})")
        return -1
    return np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2))

# 使用絕對路徑載入資料庫
db_file = os.path.abspath("output/train_features.db")
# print(f"使用絕對路徑載入資料庫：{db_file}")
if not os.path.exists(db_file):
    # print(f" 錯誤：找不到 SQLite 資料庫！請確認檔案是否存在 -> {db_file}")
    sys.exit(1)
prototypes = load_prototypes_from_database(db_file)
if not prototypes:
    # print("⚠️ 錯誤：未能載入任何原型特徵，請檢查資料庫內容！")
    sys.exit(1)
# print(f" 已載入 {len(prototypes)} 個類別的原型特徵")

# 比對傳入的特徵向量與資料庫中的原型
best_match = {"label": None, "similarity": -1.0}
for label, prototype_data in prototypes.items():
    prototype_feature = prototype_data["feature"]
    # print(f"{label} 的原型特徵向量維度: {prototype_feature.shape}")
    
    similarity = cosine_similarity(feature_vector, prototype_feature)
    # 將相似度轉換成 Python float 以確保 json.dumps 可序列化
    if similarity > best_match["similarity"]:
        best_match = {"label": label, "similarity": float(similarity)}

# 輸出結果，讓 Express.js 能夠接收
print(json.dumps(best_match, ensure_ascii=False))
sys.stdout.flush()