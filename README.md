BeePasture （蜜蜂牧场）
==========
蜜蜂牧场是一个数据采集清洗工具，也是一个ETL工具，同时也是一套脚本语言。最初只是完成网页数据采集清洗入库的工作。随着它的发展，功能越来越多。拥有同步和异步两种模式。结合apache camel，可以很方面的创建异步流式处理集群，称为牧场蜂群模式。使用这套脚本，根据任务情况，需要有xml，yaml，json，xpath, jsonpath, beetl模板，apache camel等相关知识。脚本本身是yaml格式。支持单步调试。   

request: jdk1.7   
详细文档参见wiki   
   
特性
==========
1. 支持同步(批处理)和异步（流处理）两种模式
2. 支持各种常见的数据来源的读写（http, mqtt, activemq, file, mysql, oracle, sqlserver, mongodb, elastiasearch），能很方面的扩展新的来源。
3. 集成apache camel，可以执行camel的xml脚本。beepasture的yaml脚本与camel脚本可以通过endpoint，比如seda, direct等相互调用。
4. 内嵌支持xpath, jsonpath, regex, javascript, beetl等技术。
5. 可以通过img标签的image-data属性保存图片内容。
6. 支持上传阿里OSS，并将url修改为阿里OSS的地址。
7. 丰富的过滤器方式，比如>2, <10, 2-10 表示处理那几条记录。也可以写正则匹配或脚本来判断是否处理。
8. 内部集成IK分词器
9. 支持一些常用算法，比如：DFA，Nagao，TFNeighbor等。
10. 通过camel和mq可以组成微服务处理链，也就是蜂群功能。
11. 通过和我的另一个项目蝴蝶（一个使用chrome内核开发的采集和自动化操作网页工具）配合。（在初步开发中）
   
   
BeePasture-core
------
命令行运行   
用法：     
		`java -jar BeePasture-core-1.0.jar script.yaml` <br>
指定脚本文件的encoding    
		`java -DfileEncoding=utf8 -jar BeePasture-core-1.0.jar script.yaml` <br>
指定一直执行，不退出。    
		`java -d -jar BeePasture-core-1.0.jar script.yaml` <br>
指定调试模式执行，带控制台命令，不退出。 <br>
		`java -Ddebug=true -jar BeePasture-core-1.0.jar script.yaml` <br>
脚本样例在`examples`目录中，比如：mongodb_city.yaml  \n
这是一个采集大众点评网上所有的城市的脚本，结果存入mongodb中。  
``` mongodb_city
var:
    cityUrlList: "A..Z http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${sys.currentTimeMillis()}&do=getByPY&firstPY=${i}"
    
resource: 
    mongo1:
        url: mongodb://localhost:27017/test
        

gather:
  # 获取城市列表
  - url: "http://www.dianping.com/citylist"
    xpath: "//div[@class='terms']/a"
    save: 
      to: city
      property: 
        title: //text()
        name: 
            xpath: @href
            templete: "${it.substring(1)}"
  # 获取城市列表 更多
  - url: cityUrlList
    extract: 
      - jsonpath: $.msg.html
      - xpath: //a
    save:
      to: city
      append: true
      property:
          title: //text()
          name: 
            xpath: @href
            script: "${it.substring(1)}"
            
#保存
persist:
    city: 
        resource: mongo1
```
# 脚本简单说明：
脚本为yaml格式。顶级key有以下四种:<br>
* `resource`:  定义各种源，包括文件，数据库等，比如mysql, mongodb, file 等。定义了资源的访问参数，是资源读写的入口。<br>
* `var`: 定义变量，无需说明类型，脚本中也可以不定义而直接使用。<br>
* `gather`：脚本执行的主体，定义处理步骤。<br>
* `persist`: 保存，可以保存到resource中指定的源中<br>


# resource：
包含子key是资源名称，资源种类有 file，dir，jdbc，mongodb ...
``` resource
resource:   
	#名称，可以是 字符，数字，_ 的组合
	mydb:   
		url: "jdbc:jtds:sqlserver://localhost:1433/test"  
		username: sa  
		password: "12345678"  
	oneFile:   
		url: "file://D:/temp/CSs.SQL?format=yaml"  
```
resource中的资源类型是以url参数中的schema部分指定的。支持的schema有：`jdbc,file,dir,mongodb`  

# var：   
变量只有一种，所有变量都是数组，如果前一步得到的不是数组，就会放进数组，这个数组的size是1，数组里只可以是Map或String，如果数组里是Map，那么Map的key就是property
--------
``` var
var：
    cityList: []
    cityUrlList: "A..Z http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${sys.currentTimeMillis()}&do=getByPY&firstPY=${i}"
```
`[]`表示数组  
`n..n somestring${i}`也是数组
上例是大众点评网上所有的城市列表，是个有26个成员的数组，其中${i}分别是A,B,C,D...Z。    
`${sys.currentTimeMillis()}`为当前时间戳
`n..n`是数组中的不同部分的定义，可以是字母，比如：A..Z 和 a..g，也可以是数字，比如：5..100。  
抓取大众点评网所有城市的脚本见 examples/mongodb_city.yaml

# gather
脚本执行的主体，定义处理步骤，每一个步骤都是数组中的一员，步骤按顺序执行（2.0版中，变量将变成队列，每一个步骤将会是同时执行，队列中有数据就拿出来处理），可以使用xpath, jsonpath, 脚本（beetl）
h3. gather命令结构
* url 采集的url地址，可以包含脚本比如：${date(), "yyyy-MM-dd"}表示当前日期，${dateAdd(-30),dateFormat="yyyy-MM-dd"}表示三十天前的日期。 
* extract 清洗语句，其value为数组，数组成员为一对key:value。key为处理方式：xpath, jsonpath, regex, script。value是其值。
        * xpath: xpath表达式
        * jsonpath: jsonpath表达式
        * regex: regex表达式
        * script: 脚本模板
* post 采用post方法，值为提交上去的内容。
* download 如果是下载文件的url，这里指定下载到本地的文件名，放在to下边 
	* to 下载到本地的文件名。 
	* filename 文件名将放入那个变量中。 
* charset 如果是文本文件，可以在这里描述文件的charset。 
* Content-Encoding 设置为gzip或者不设置
* limit 对url列表中执行的个数限制。 
* sleep 每执行一个url，指定sleep多少毫秒。 
* direct true: 表示直接将url的内容付给返回值。false: 表示将url作为http的地址从网上爬取网页内容付给返回值。
* xpath 对采集下来的页面内容用xpath过滤出数据。支持jsonpath，语法：json(path express),比如：json($}表示取json的根节点。
* regex 正则表达式过滤数据，先执行xpath，再执行regex
* script 脚本模板，先执行xpath，再执行regex，最后执行script
* save 保存到变量 
	* to 保存到的变量名 
	* property 变量的property 
		* [key] 这个key直接写property的名称，也就是map的key名。他的值就是key的value。可以直接写xpath表达式（缺省）。 
			* scope _this 或 _page 或 it。 缺省: it
			* script value用脚本来运算得出。 
		        * regex 正则表达式，针对当前变量。
		        * script 脚本 变量说明
			    * it 经过xpath定位后的网页内容
			    * _page 通过url采集下来的原始网页
			    * _this url的内容，如果有with语句，就是with指定的变量。
	* encoding 编码 
* with 指定缺省变量，如果指定了缺省变量，那么这个步骤中可以直接使用这个变量的property名。 

# persist

# script的函数说明
* it,_this, _page 为String类型，可以直接使用String的方法，比如：${it.substring(0, it.lastIndexOf("...") + 3)}
* str函数 字符串函数，比如：str.urlDecode(str, "utf8")，函数列表：
	* String now(String pattern) 返回当前时间字符串，pattern为日期格式，比如: yyyy-MM-dd
	* String trim(String str)
	* String substring(String str, int beginIndex)
	* String substring(String str, int beginIndex, int endIndex)
	* int indexOf(String str, String indexOfStr)
	* String unicode2str(String str)
	* Date smartDate(String str)
	* String urlEncod(String str, String charset)
	* String urlDecode(String str, String charset)
	* String scheme(String str)
	* String authority(String str)
	* String host(String str)
	* Integer port(String str)
	* String queryStr(String str)
	* Map query(String str)  得到url中的参数对，比如要得到url中page参数: ${str.query(_this)["page"]}
	* String regex(String str, String regex, int n)
	* String regex(String str, String regex)

BeePasture-camel
------
是异步执行的版本，是以流的方式执行，flow关键字下面的所有步骤都是单独线程同时执行的，就行工厂里的流水线一样。这个工具可以一次性执行。也可以作为demo进程提供服务，比如监听mq的topic，然后通过步骤执行业务逻辑。


===
