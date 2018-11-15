//定义业务服务
app.service("seckillOrderService",function ($http) {
    //加载列表数据
    this.findAll = function(){
        return $http.get("../seckillOrder/findAll.do");
    };

    this.findPage = function (page, rows) {
        return $http.get("../seckillOrder/findPage.do?page=" + page + "&rows=" + rows);
    };

    this.add = function (entity) {
        return $http.post("../seckillOrder/add.do",entity);
    };

    this.update = function (entity) {
        return $http.post("../seckillOrder/update.do",entity);
    };

    this.findOne = function (id) {
        return $http.get("../seckillOrder/findOne.do?id=" + id);
    };

    this.delete = function (selectedIds) {
        return $http.get("../seckillOrder/delete.do?ids=" + selectedIds);
    };

    this.search = function (page, rows, searchEntity) {
        return $http.post("../seckillOrder/search.do?page=" + page + "&rows=" + rows, searchEntity);

    };
});