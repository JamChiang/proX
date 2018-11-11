app.controller("cartController", function ($scope, cartService) {

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

    //加入购物车
    $scope.addItemToCartList = function (itemId, num) {
        cartService.addItemToCartList(itemId, num).success(function (response) {
            if(response.success){
                //刷新列表
                $scope.findCartList();
            } else {
                alert(response.message);
            }

        });

    };
});