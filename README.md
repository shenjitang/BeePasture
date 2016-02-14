# BeePasture
BeePasture:蜜蜂牧场是一个简单易用的数据ETL工具。支持各种传统的关系型数据库比如mysql, oracle, sqlserver，mongodb等nosql数据库，网页数据抓取，文件。支持json,xml,html等数据格式，支持xpath, jsonpath, beetl脚本模板。任务是通过简单的yaml脚本编写的。
request: jdk1.7
execute:
java -jar bin/webgather-web-1.0.jar webgather4.yaml

set file encoding default is GBK
java -DfileEncoding=utf8 -jar bin/webgather-web-1.0.jar webgather4.yaml

script file is yaml language. 
sample: webgather1.yaml,webgather2.yaml,webgather3.yaml,webgather4.yaml

脚本样例在目录examples中。
