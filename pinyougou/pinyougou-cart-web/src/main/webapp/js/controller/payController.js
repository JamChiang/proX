app.controller("payController", function ($scope, $location, cartService, payService) {

    $scope.getUsername = function () {
        cartService.getUsername().success(function (response) {
            $scope.username = response.username;

        });

    };

    //生成二维码
    $scope.createNative = function () {
        //获取支付日志id
        $scope.outTradeNo = $location.search()["outTradeNo"];

        payService.createNative($scope.outTradeNo).success(function (response) {
            if("SUCCESS"== response.result_code){
                //如果生成订单成功；则生成二维码
                $scope.totalFee = (response.total_fee/100).toFixed(2);

                //创建二维码图片
                var qr = new QRious({
                    element:document.getElementById("qrious"),
                    level:"Q",
                    size:250,
                    value:response.code_url
                });

                //查询支付状态
                queryPayStatus($scope.outTradeNo);

            } else {
                alert("生成支付二维码失败");
            }

        });

    };

    //查询支付状态
    queryPayStatus = function (outTradeNo) {
        payService.queryPayStatus(outTradeNo).success(function (response) {
            if(response.success){
                //跳转到支付成功页面
                location.href = "paysuccess.html#?money=" + $scope.totalFee;
            } else {
                if (response.message == "二维码超时") {
                    alert(response.message);
                    //重新生成二维码
                    $scope.createNative();
                } else {
                    //如果支付失败则跳转到支付失败页面
                    location.href = "payfail.html";
                }
            }

        });

    };

    //支付成功后加载显示支付金额
    $scope.getMoney = function () {
        $scope.money = $location.search()["money"];
    };

});