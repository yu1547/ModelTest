var express = require("express");
const { spawn } = require("child_process");  // 🔹 使用 `child_process` 來執行 Python
var router = express.Router();
const path = require("path");
const pythonScriptPath = path.resolve(__dirname, "../compare_test.py");  // 🔹 確保路徑正確
const workingDirectory = path.resolve(__dirname, "../");
// 打印當前工作目錄以作確認
console.log("Express.js 的工作目錄:", process.cwd());
console.log("設定的 spawn 工作目錄:", workingDirectory);

router.post("/process-vector", function (req, res) {
    const featureVector = req.body.vector;
    console.log("🔎 收到特徵向量:", featureVector);
    console.log("工作目錄:", __dirname);
    console.log("目前工作目錄:", process.cwd());
    // const featureVector = [
    //     -1.4695204, -0.610416, -0.63751274, 0.86774486, -0.28947118, -0.68347198, -0.18429366, -0.51768064,
    //     1.7752104, -1.0443141, -0.28096586, 0.22967619, 1.4362332, 0.89772505, 0.7532197, -0.8432954,
    //     -0.5540246, -1.1753281, -2.2896526, -1.5719123, -0.28654906, -1.1622961, -0.15807864, 0.298978,
    //     0.49146256, -1.6191038, 0.9298653, 0.12743329, -0.13400313, -1.2398878, -0.46068197, -0.89387518,
    //     -0.470083, 0.26365042, -1.0588495, -0.50422895, -1.3349142, 1.3157506, -0.0060072867, -0.97476524,
    //     0.34556946, 0.045259804, -0.63122928, 1.1429211, -1.507483, 0.1764448, -1.6441997, 1.0451773,
    //     1.0009202, 0.44408354, -1.5843607, -0.7848587, -0.70419538, 0.021052783, -0.60741687, 0.13763286,
    //     0.89226931, -1.9781638, -0.59097242, 0.01763637, 2.4441257, -0.15427278, 0.18149583, -0.2811524,
    //     0.40551618, -0.53443432, -0.36272031, 0.72740412, -0.08683534, 0.51878339, -1.6919171, -3.31529,
    //     0.31330854, -0.45148426, 0.064268209, 0.29503283, 0.68297338, -0.035677217, -0.85037637, -0.86635679,
    //     -0.90656275, 0.23354429, 2.0188003, -0.12048066, 1.8737234, 1.4208332, 0.90143704, -0.80313021,
    //     0.92001826, 0.88673526, 1.6149085, 1.4966887, -1.846485, -0.024569286, 0.5891698, 0.77232438,
    //     -0.20152529, 0.17579004, -1.3836521, 0.17451061, -0.63706338, -0.32270721, -1.6942583, 0.56751436,
    //     0.39628023, -1.8744233, 1.6389489, 0.73142159, -3.4266615, -2.9089339, -0.021535601, 0.22721633,
    //     0.5924992, -1.2323098, -0.79150373, -1.4483074, 0.0021662889, -1.332961, -0.66045654, 0.77427375,
    //     0.24077202, -0.82813728, 0.23518144, 0.15156801, 0.66846931, -1.6919734, 0.2245989, -0.20810546
    // ];
    // 執行 Python 腳本，並傳遞特徵向量
    const pythonProcess = spawn("python", [pythonScriptPath, JSON.stringify(featureVector)], { cwd: workingDirectory });

    let outputData = "";  // 🔹 收集 stdout 的完整字串

    pythonProcess.stdout.on("data", (data) => {
        if (!res.headersSent) {  // 🔹 確保只送一次回應
            console.log("✅ Python 回應: " ,data.toString('utf8'));
            outputData += data.toString('utf8');
        }
    });
    
    pythonProcess.stderr.on("data", (data) => {
        console.error("❌ Python 錯誤:", data.toString('utf8'));
        if (!res.headersSent) {
            res.status(500).json({ error: "Python 執行錯誤", details: data.toString('utf8') });
        }
    });

    pythonProcess.on("close", () => {
        console.log(outputData);  // ✅ 在後端終端輸出完整 log
        const lines = outputData.trim().split("\n");

        // 🔍 嘗試找出 Top 結果部分並轉為 JSON 格式回傳
        const resultList = lines.filter((line) => line.startsWith("Top")).map((line) => {
            const match = line.match(/Top (\d+): 類別=(.*?), 相似度=([\d.]+), 圖片=(.*)/);
            if (match) {
                return {
                    rank: parseInt(match[1]),
                    label: match[2],
                    similarity: parseFloat(match[3]),
                    path: match[4],
                };
            }
        }).filter(Boolean);

        res.json({ status: "比對完成", result: resultList });
    });
});

module.exports = router;
