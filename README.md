![bowser](http://leviathyn.com/wp-content/uploads/2013/01/Bowser_picture-555x472.png)

## Creating a webserver

We're going to make an Eskimo dating website. We can start up the webserver with the following line of code.

```java
  new WebServer("Eskimo Friends", 80, true).start();
```

The default 'title' of each page will be 'Eskimo Friends'. The server will run on port 80. And we'll be running it in 'developer mode'. This means that pages and other resource will NOT be cached. This is useful because now you can change something and immediately hit F5 in your browser to see your changes without restarting the server.

Now, the way things currently are, every single page you try to go to will result in a 404: Page Not Found error.

In the next section, we'll go over how to add controllers to handle web requests.

## A Basic Controller

Here's an example Controller.

```java
public class HomePage extends Controller {

  @Override
  public void init(){
    route("GET", "/home").to("home.html");
  }

}
```

Whenever a web browser does a GET request on /home, the server will respond with the contents of home.html. You should place the home.html file in the same folder as the controller which is serving it.

Now, when constructing the webserver, we must register our HomePage controller like so:

```java
    WebServer server = new WebServer("Eskimo Friends", 80, true)
        .controller(new HomePage())
        .start();
```
