app.controller("orderInfoController", function ($scope,addressService, cartService) {

    $scope.getUsername = function () {
        cartService.getUsername().success(function (response) {
            $scope.username = response.username;

        });

    };

    //查询购物车列表
    $scope.findCartList = function () {
        cartService.findCartList().success(function (response) {
            $scope.cartList = response;

            //计算总数量和总价
            $scope.totalValue = cartService.sumTotalValue(response);
        });

    };


    //查询当前登录用户的地址列表
    $scope.findAddressList = function () {
        addressService.findAddressList().success(function (response) {
            $scope.addressList = response;

            //获取默认的地址
            for (var i = 0; i < response.length; i++) {
                var address = response[i];
                if ("1" == address.isDefault) {
                    $scope.address = address;
                    break;
                }
            }
        });

    };

    //选择收件人地址
    $scope.selectAddress = function (address) {
        $scope.address = address;

    };

    //判断地址是否为当前选中的地址
    $scope.isSelectedAddress = function (address) {
        return $scope.address == address;

    };

    //默认订单的支付方式--微信支付
    $scope.order = {paymentType:"1"};

    //选择支付方式
    $scope.selectPaymentType = function (type) {
        $scope.order.paymentType = type;

    };

    //提交订单
    $scope.submitOrder = function () {

        //设置订单收件人信息
        $scope.order.receiverAreaName = $scope.address.address;
        $scope.order.receiverMobile = $scope.address.mobile;
        $scope.order.receiver = $scope.address.contact;

        cartService.submitOrder($scope.order).success(function (response) {
            if(response.success){
                if("1" == $scope.order.paymentType){
                    //如果是微信支付则跳转到支付页面
                    location.href = "pay.html#?outTradeNo=" + response.message;
                } else {
                    //如果是货到付款则跳转到支付成功页面
                    location.href = "paysuccess.html";
                }
            } else {
                alert(response.message);
            }

        });

    };
});