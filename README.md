![bowser](https://leviathyn.com/wp-content/uploads/2013/01/Bowser_picture-555x472.png)

## Introduction

First of all, I want to describe the motivation for this project given that there are already many java web servers out there. In my opinion, Jetty and Tomcat are bloated and have terrible APIs. Spring made me want to shoot myself. If you enjoy XML configuration files, endless annotations, and AntiFactoryPatternCreatorDelegators, you may like using one of those aforementioned frameworks. If you want something incredibly simple and elegant, use SimpleFramework. If you want to use something like SimpleFramework, but with routing and templating on top, this library might be for you.

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

    // check the username and password against the database
    // in this example, assume we have something called 'userDB'

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
    // for this example, let's assume that we have databases called 'userDB' and 'messageDB'

    // get the user making this request
    User user = userDB.getUser(context.request.param("token"));

    List<Message> messages = messageDB.getMessages(user.id);

    // when you put something into the context, it becomes accessible by the HTML.
    context.put("user", user);
    context.put("messages", messages);
  };

}
```

And here is the HTML that uses the data that we've put into the context.

```html
<h1>Hello $$(user.firstName)</h1>
<h2>You have $$(messages.size()) messages.</h2>
<div loop="message in messages" class="message">
  <p>$$(message.title)</p>
  <p>$$(message.body)</p>
</div>
```

## Super fancy template examples

So far you've seen that you can loop through objects, that you can insert variables, and that you can call size() on a collection. In this example, we'll go through all the other random things that Bowser supports.

### Link and embed resources into the page

```html
<!-- template -->
<body>
  <js src="fancy1.js https://cdndomain/library.js fancy2.js" />
  <css src="style1.css style2.css" />

  <p>First block of body content</p>
  <js defer src="fancy3.js" />
  <js inline src="fancy4.js" />

  <p>Second block of body content</p>
  <js src="module1.mjs" />
  <js inline src="module2.mjs" />

  <css print src="print.css" />

  <div class="content-from-another-file">
    <import html="file.html" />
  </div>

  <p>
    You can also hoist any content to the head by using a head tag inside the
    body:
  </p>
  <head>
    <meta name="theme-color" content="#ffffff" />
    <script>
      const a = "b";
    </script>
  </head>
  <div>Last element on the page</div>
</body>

<!-- output -->
<head>
  <meta name="theme-color" content="#ffffff" />
  <link href="style1.css" rel="stylesheet" media="screen" />
  <link href="style2.css" rel="stylesheet" media="screen" />
  <link href="print.css" rel="stylesheet" media="print" />
  <script src="fancy1.js"></script>
  <script src="https://cdndomain/library.js"></script>
  <script src="fancy2.js"></script>
  <script src="fancy3.js" defer></script>
  <script src="module1.mjs" type="module"></script>
  <script>
    const a = "b";
  </script>
</head>
<body>
  <p>First block of body content</p>
  <script>
    // the content of fancy4.js
  </script>

  <p>Second block of body content</p>
  <script type="module">
    // the content of module2.mjs
  </script>

  <div class="content-from-another-file">
    <div>Contents of file.html</div>
  </div>

  <p>
    You can also hoist any content to the head by using a head tag inside the
    body:
  </p>
  <div>Last element on the page</div>
</body>
```

### Insert variables into your page

```html
<p>Insert a variable: $$(user.name)</p>
<p>Call a method: $$(user.getAddress())</p>
<div if="items.hasData()">
  <h2>You have $$(items.size()) items.</h2>
  <span loop="item in items">$$(item.name)</span>
</div>
<p if="items.isEmpty()">You have no items.</p>
<p if="!items.isEmpty()">Another way of saying hasData()</p>

<script>
  // in javascript, this is how you replace variables
  let dynamic = "Hello $$(user.name)!";
</script>
```

## Websockets

You can start a websocket server like so:

```java
 int port = 12345;
 new WebSocketServer(port).onOpen(socket->{
   System.out.println("Client connected: " + socket);
 }).start();
```
