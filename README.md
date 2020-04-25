# MasterApp
The master app helps to perform following steps: \
1.sets up NSD Manager,sets up DiscoveryListener \
2.sets up ResolveListener \
3.Resolve Service if found \
4.Get Server/Host IP Address and port \
5.Resolve the service and call a function to connect to the server. \
6.Create a JSONObject with fields request and ipAddress. \
7.Create a new thread using AsyncTask for the Socket connection so that UI Thread doesn’t hang \
8.In AsyncTask thread, create Socket connection. Send JSONObject as String and wait for Server’s repsonse. 
