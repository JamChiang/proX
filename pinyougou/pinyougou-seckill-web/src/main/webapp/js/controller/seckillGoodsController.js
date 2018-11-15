app.controller("seckillGoodsController", function ($scope, $location, $interval, seckillGoodsService) {

    //获取秒杀商品列表
    $scope.findList = function () {
        seckillGoodsService.findList().success(function (response) {
            $scope.list = response;
        });
    };

    $scope.findOne = function () {
        seckillGoodsService.findOne($location.search()["id"]).success(function (response) {
            $scope.entity = response;

            //倒计时总秒数
            var allSeconds = Math.floor((new Date(response.endTime).getTime() - new Date().getTime()) / 1000);

            //angularJS的定时服务对象；参数1：要执行的方法，参数2：执行的间隔
            var task = $interval(function () {
                if (allSeconds > 0) {
                    allSeconds = allSeconds - 1;
                    //转换倒计时总秒数为 **天**:**:** 的格式并在页面展示
                    $scope.timestring = convertTimeString(allSeconds);
                } else {
                    $interval.cancel(task);
                    alert("秒杀活动已结束。");
                }
            }, 1000);
        });
    };

    convertTimeString = function (allSeconds) {
        //天数
        var days = Math.floor(allSeconds / (60 * 60 * 24));
        //时
        var hours = Math.floor((allSeconds - days * 60 * 60 * 24) / (60 * 60));
        //分
        var minutes = Math.floor((allSeconds - days * 60 * 60 * 24 - hours * 60 * 60) / 60);
        //秒
        var seconds = allSeconds - days * 60 * 60 * 24 - hours * 60 * 60 - minutes * 60;

        var str = "";
        if (days > 0) {
            str = days + "天";
        }
        return str + hours + ":" + minutes + ":" + seconds;
    };
	
    //提交秒杀订单
    $scope.submitOrder = function () {
        seckillGoodsService.submitOrder($scope.entity.id).success(function (response) {
            if(response.success){
                alert("提交订单成功；请在1分钟内完成支付");
                location.href = "pay.html#?outTradeNo=" + response.message;
            } else {
                alert(response.message);
            }
        });
    };
});