var exec = require('cordova/exec');

exports.detect = function (arg0, success, error) {
    exec(success, error, 'FaceRecognition', 'detect', [arg0]);
};

exports.compare = function (mainImage, itemImage, success, error) {
    var data = [];
    data.push({
        mainImage: mainImage,
        itemImage: itemImage,
    })
    exec(success, error, 'FaceRecognition', 'compare', data);
};
