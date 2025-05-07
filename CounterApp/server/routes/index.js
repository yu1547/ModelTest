var express = require("express");
var router = express.Router();

router.post("/process-vector", function (req, res) {
    const featureVector = req.body.vector;

    console.log("收到特徵向量:", featureVector);  // 確認前端送來的資料
    res.json({ status: "收到特徵向量", vector: featureVector });
});

module.exports = router;
