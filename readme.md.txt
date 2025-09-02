In VS Code terminal:

mvn clean package
mvn azure-functions:run
Or just press F5 (Run → Start Debugging).
The Azure Functions extension will attach the debugger.

Test in browser/terminal:

http://localhost:7071/api/HttpExample?name=Krishna