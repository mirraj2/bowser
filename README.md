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

## Handling POST requests

Let's say the user wants to login. Here's how we'd handle that.

```java
public class HomePage extends Controller {

  @Override
  public void init(){
    route("GET", "/home").to("home.html");
    route("POST", "/login").to(login);
  }
  
  private final Handler login = (request, response) -> {
    String username = request.param("username");
    String password = request.param("password");
    
    //check the username and password against the database
    //in this example, assume we have something called 'userDB'
    
    if(userDB.isValidLogin(username, password)){
      response.cookie("token", userDB.generateToken());
    } else {
      throw new RuntimeException("Invalid login credentials.");
    }
  };

}
```

## Dynamic Pages (Templates)

Alright, here are where things get interesting. Let's say that users of our site have 'messages' and we want to display those messages to them. The controller will supply the data and the html will render that data.

```java
public class MessagesPage extends Controller {

  @Override
  public void init(){
    route("GET", "/messages").to("messages.html").data(messages);
  }
  
  private final Data messages = context -> {
    //for this example, let's assume that we have databases called 'userDB' and 'messageDB'
  
    //get the user making this request
    User user = userDB.getUser(context.request.param("token"));
  
    List<Message> messages = messageDB.getMessages(user.id);
    
    //when you put something into the context, it becomes accessible by the HTML.
    context.put("user", user);
    context.put("messages", messages);
  };

}
```

And here is the HTML that uses the data that we've put into the context.

```html
<h1>Hello {user.firstName}</h1>
<h2>You have {messages.size()} messages.</h2>
<div loop="message in messages" class="message">
  <p>{message.title}</p>
  <p>{message.body}</p>
</div>
```

## A super fancy template example

So far you've seen that you can loop through objects, that you can insert variables, and that you can call size() on a collection. In this example, we'll go through all the other random things that Bowser supports.

```html
<head js="fancy.js" css="fancy.css">
<p>The text above makes allows us to import javascript and css files into the 'head'</p>
<p>Insert a variable: {user.name}</p>
<p>Call a method: {user.getAddress()}</p>
<div if="items.hasData()">
  <h2>You have {items.size()} items.</h2>
  <span loop="item in items">{item.name}</span>
</div>
<p if="items.isEmpty()">You have no items.</p>
<p if="!items.isEmpty()">Another way of saying hasData()</p>
<script>
  var text = "In javascript, there is how you replace variables.";
  var dynamic = "Hello $${user.name}";
  var text2 = "It is just like in the HTML except with two dollar signs. Someday I'll change the way this is escaped to something better :p";
</script>
<p>You can also import javascript/html from other files like this:</p>
<import js="mario.js">
<import html="chat-widget.html">
```
