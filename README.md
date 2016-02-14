# BeePasture
BeePasture:蜜蜂牧场是一个简单易用的数据ETL工具。支持各种传统的关系型数据库比如mysql, oracle, sqlserver，mongodb等nosql数据库，网页数据抓取，文件。支持json,xml,html等数据格式，支持xpath, jsonpath, beetl脚本模板。任务是通过简单的yaml脚本编写的。
编译后运行：java -jar BeePasture-core script1.yaml
支持的环境变量:fileEncoding。比如：java -jar -DfileEncoding=utf8 BeePasture-core script1.yaml
脚本样例：
1，mysql数据导入mongodb：
resource: 
    db1: 
        url: "jdbc:mysql://localhost:3306/test"
        username: root
        password: "12345678"
    mongo1:
        url: mongodb://localhost:27017/test

var:
    hello_man: []

gather:
    - url: db1
      param: 
          sql: "select * from hello_man"
      save: 
          to: hello_man
        
persist:
    hello_man: 
        resource: mongo1



2, 抓取dailystock的数据
resource: 
    db1: 
        url: "jdbc:mysql://localhost:3306/test"
        #databaseName: test
        username: root
        password: "12345678"

var:
  stockList: []

gather:
  - url: "http://www.shfe.com.cn/data/dailydata/20151029dailystock.dat"
    encoding: utf8
    xpath: json($)
    save: 
      to: stockList

#保存
persist:
    stockList.o_cursor: 
        resource: db1
        sql: "insert into tstock (VARNAME, WRTWGHTS, print_date) values ('${o_cursor.VARNAME}', '${o_cursor.WRTWGHTS}', '${stockList.print_date}')"


3, 抓取大众点评网的城市列表和商店列表
queue:
    # 城市列表
    cityList :
    shopList :
# 获取城市列表 http://www.dianping.com/citylist
gather:
  - url: "http://www.dianping.com/citylist"
    sleep: 1000
    xpath: "//div[@class='terms']/a/@href"
    templete: "http://www.dianping.com${page}"
    save: 
      to: cityList
# 获取城市title信息 cityList
  - url: cityList
    limit: 10
    xpath: "//head/title/text()"
    save: 
      to: shopList


#保存
persist:
    cityList: "file://d:/temp/cityList-${date(),dateFormat=\"yyyy-MM-dd\"}.txt?format=planttext&encoding=utf8"
    shopList: "file://shopList-${date(),dateFormat=\"yyyy-MM-dd\"}.txt?format=planttext&encoding=utf8"
