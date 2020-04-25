# MasterApp
The master app helps to perform following steps: \
1.sets up NSD Manager\ 
2.sets up DiscoveryListener \
3.sets up ResolveListener \
4.Resolve Service if found \
5.Get Server/Host IP Address and port \
6.Resolve the service and call a function to connect to the server. \
7.Create a JSONObject with fields request and ipAddress. \
8.Create a new thread using AsyncTask for the Socket connection so that UI Thread doesn’t hang \
9.In AsyncTask thread, create Socket connection. Send JSONObject as String and wait for Server’s repsonse. 
