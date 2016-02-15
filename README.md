BeePasture
==========
蜜蜂牧场是一个简单易用的脚本形式的数据ETL工具。支持各种传统的关系型数据库比如mysql, oracle, sqlserver，mongodb等nosql数据库，网页数据抓取，文件。支持json,xml,html等数据格式，支持xpath, jsonpath, beetl脚本模板。脚本是通过简单的yaml编写的。<br>
request: jdk1.7<br>

BeePasture-core
------
命令行运行工具<br>
用法： <br>
		`java -jar BeePasture-core-1.0.jar webgather4.yaml` <br>
指定脚本文件的encoding <br>
		`java -DfileEncoding=utf8 -jar BeePasture-core-1.0.jar webgather4.yaml` <br>
脚本样例在`examples`目录中
<br>
<br>
# 脚本简单说明：
脚本为yaml格式。顶级key有以下四种:<br>
1. `resource`:  定义各种源，包括文件，数据库等，比如mysql, mongodb, file 等。定义了资源的访问参数，是资源读写的入口。<br>
2. `var`: 定义变量，无需说明类型，脚本中也可以不定义而直接使用。<br>
3. `gather`：定义数据，可以使用xpath, jsonpath, 脚本（beetl）<br>
4. `persist`: 保存，可以保存到resource中指定的源中<br>


# resource：
包含子key是资源名称，资源种类有 file，dir，jdbc，mongodb ...
		resource:   
				mydb:   
				url: "jdbc:jtds:sqlserver://localhost:1433/test"  
				username: sa  
				password: "12345678"  
		oneFile:   
				url: "file://D:/temp/CSs.SQL?format=yaml"  


变量定义支持数组，比如：
var：
    cityUrlList: "A..Z http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=${i}"
这个是大众点评网上所有的城市列表，是个有26个成员的数组，其中${i}分别是A,B,C,D...Z。    ${time}为当前时间戳，
x..x是数组中的不同部分的定义，可以是字母，比如：A..Z 和 a..g，也可以是数字，比如：5..100。
抓取大众点评网所有城市的脚本见 examples/mongodb_city.yaml

BeePasture-grizzly2
------
是sun的jersey框架的restful服务。脚本通过http post执行。

