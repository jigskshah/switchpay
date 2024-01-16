library switchpayment;

import 'package:flutter/services.dart';

class SwitchPayment{

  String orderId;
  String baseUrl;

  double amount;
  String description;

  String name;
  String mobileNumber;
  String email;

  String token;
  String uuid;

  String? otherInfo;


  SwitchPayment({
    required this.orderId,
    required this.baseUrl,

    required this.amount,
    required this.description,

    required this.name,
    required this.mobileNumber,
    required this.email,

    required this.token,
    required this.uuid,

    this.otherInfo,
  });

  void openPaymentPage({
    required Function Function(String transactionId) onSuccess,
    required Function Function() onFailure,
    required Function Function(dynamic error) onError,
  }) {
    MethodChannel methodChannel = const MethodChannel('upi_channel');

    var arguments = {
      "amount": amount.toString(),
      "description": description,
      "name": name,
      "mobile": mobileNumber,
      "email": email,
      "bearer_token": "Bearer $token",
      "user_uuid": uuid,
      "base_url": baseUrl,
      "order_id": orderId,
    };

    methodChannel.invokeMethod("upi", arguments).onError((error, stackTrace) => {
      onError(error)
    }).then((response) {
      String status = "failed";
      String transactionId = "";

      if (response != null) {
        status = response!["status"] ?? 'failed';
        transactionId = response["transaction_id"]!;
        if (status == "captured") {
          onSuccess(transactionId);
        }
        else if(status == "failed"){
          onFailure();
        }
      }
      else {
        onError("Something went wrong");
      }

      // print("transactionId : -----> $transactionId");
      // print("status : -----> $status");


    });
  }

}


