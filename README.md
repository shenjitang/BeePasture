# BeePasture
BeePasture:蜜蜂牧场是一个简单易用的数据ETL工具。支持各种传统的关系型数据库比如mysql, oracle, sqlserver，mongodb等nosql数据库，网页数据抓取，文件。支持json,xml,html等数据格式，支持xpath, jsonpath, beetl脚本模板。任务是通过简单的yaml脚本编写的。
request: jdk1.7
execute:
java -jar binBeePasture-core-1.0.jar webgather4.yaml

set file encoding default is GBK
java -DfileEncoding=utf8 -jar BeePasture-core-1.0.jar webgather4.yaml

script file is yaml language. 
sample: webgather1.yaml,webgather2.yaml,webgather3.yaml,webgather4.yaml

脚本样例在目录examples中。

BeePasture-grizzly2是sun的jersey框架的restful服务。脚本通过http post执行。
脚本分为四大部分
1，resource:  定义各种源，包括文件，数据库等，比如mysql, mongodb, file 等。
2，var: 定义变量，无需说明类型，脚本中也可以不定义而直接使用。
3，gather：定义数据，可以使用xpath, jsonpath, 脚本（beetl）
4，persist: 保存，可以保存到resource中指定的源中

支持n..n形式的数组，比如：
var：
    cityUrlList: "A..Z http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=${i}"
这个是大众点评网上所有的城市列表，是个有26个成员的数组，其中${i}分别是A,B,C,D...Z。    ${time}为当前时间戳，
抓取大众点评网所有城市的脚本见 examples/mongodb_city.yaml
