const functions = require('firebase-functions');
const axios = require("axios");

function sendFCM(response, isLightOn) {
    const postData = {
        "to": "/topics/light",
        "collapse_key": "type_a",
        "data": {
            "light": Boolean(isLightOn)
        }
    };
    const config = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'key=AAAADM97SQ0:APA91bEP5jtaaJ3gVGdvAXD3duqB3h4dPjScZj7WllCmkhM8X_gkMsEc04SthAazBqI2JfNagYd_oSXT1afgWwgZjm1BTAK-AcDsTgD_H7k8jbRiHeYAk_KasItIjeioUBOxRYA4_8yh',
        }
    };
    axios.post('https://fcm.googleapis.com/fcm/send', postData, config)
        .then((res) => {
            console.log("RESPONSE RECEIVED: ", res);
            response.send(res);
        })
        .catch((err) => {
            console.log("AXIOS ERROR: ", err);
            response.send(err);
        })
}

exports.send_fcm_light_on = functions.https.onRequest((_, response) => sendFCM(response, true));
exports.send_fcm_light_off = functions.https.onRequest((_, response) => sendFCM(response, false));
