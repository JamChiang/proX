app.service("addressService", function($http){

    this.findAddressList = function () {
        return $http.get("address/findAddressList.do");

    }
});