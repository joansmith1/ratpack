# Handlers

This chapter introduces handlers, which are the fundamental components of a Ratpack application.

## What is a handler?

Conceptually, a handler ([`Handler`](api/ratpack/handling/Handler.html)) is just a function that acts on a handling context ([`Context`](api/ratpack/handling/Context.html)).

The “hello world” handler looks like this…

```language-java
import ratpack.handling.Handler;
import ratpack.handling.Context;

public class Example implements Handler {
  public void handle(Context context) {
      context.getResponse().send("Hello world!");
  }
}
```

As we saw in the [previous chapter](launching.html), one of the mandatory launch config properties is the HandlerFactory implementation
that provides the primary handler.
The handler that this factory creates is effectively the application.

This may seem limiting, until we recognise that a handler does not have to be an _endpoint_ (i.e. it can do other things than generate a HTTP response).
Handlers can also delegate to other handlers in a number of ways, serving more of a _routing_ function.
The fact that there is no framework level (i.e. type) distinction between a routing step and an endpoint offers much flexibility.
The implication is that any kind of custom request processing _pipeline_ can be built by _composing_ handlers. 
This compositional approach is the canonical example of Ratpack's philosophy of being a toolkit instead of a magical framework.

The rest of this chapter discusses aspects of handlers that are beyond HTTP level concerns (e.g. reading headers, sending responses etc.), which is addressed in the [HTTP chapter](http.html).

## Handler delegation

If a handler is not going to generate a response, it must delegate to another handler.
It can either _insert_ one or more handlers, or simply defer to the _next_ handler.

Consider a handler that routes to one of two different handlers based on the request path. 
This can be implemented as…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;

public class FooHandler implements Handler {
  public void handle(Context context) {
    context.getResponse().send("foo");
  }
}

public class BarHandler implements Handler {
  public void handle(Context context) {
    context.getResponse().send("bar");
  }
}

public class Router implements Handler {
  private final Handler fooHandler = new FooHandler();
  private final Handler barHandler = new BarHandler();
      
  public void handle(Context context) {
    String path = context.getRequest().getPath();
    if (path.equals("foo")) {
      context.insert(fooHandler);
    } else if (path.equals("bar")) {
      context.insert(barHandler);
    } else {
      context.next();
    } 
  }    
}
```

The key to delegation is the [`context.insert()`](api/ratpack/handling/Context.html#insert-ratpack.handling.Handler...-) method that passes control to one or more linked handlers.
The [`context.next()`](api/ratpack/handling/Context.html#next--) method passes control to the next linked handler.

Consider the following…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintThenNextHandler implements Handler {
  private final String message;
  private final static Logger LOGGER = LoggerFactory.getLogger(PrintThenNextHandler.class);

  
  public PrintThenNextHandler(String message) {
    this.message = message;
  } 
  
  public void handle(Context context) {
    LOGGER.info(message);
    context.next();
  }
}

public class Application implements Handler {    
  public void handle(Context context) {
    context.insert(
      new PrintThenNextHandler("a"),
      new PrintThenNextHandler("b"),
      new PrintThenNextHandler("c")
    );
  }    
}
```

Given that `Application` is the primary handler (i.e. the one returned by the launch config's `HandlerFactory`),
when this application receives a request the following will be written to `System.out`…

```
a
b
c
```

And then what?
What happens when the “c” handler delegates to its next?
The last handler is _always_ an internal handler that issues a HTTP 404 client error (via `context.clientError(404)` which is discussed later).

Consider that inserted handlers can themselves insert more handlers…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintThenInsertOrNextHandler implements Handler {
  private final String message;
  private final Handler[] handlers;
  private final static Logger LOGGER = LoggerFactory.getLogger(PrintThenInsertOrNextHandler.class);

  public PrintThenInsertOrNextHandler(String message, Handler... handlers) {
    this.message = message;
    this.handlers = handlers;
  }

  public void handle(Context context) {
    LOGGER.info(message);
    if (handlers.length == 0) {
      context.next();
    } else {
      context.insert(handlers);
    }
  }
}

public class Application implements Handler {
  public void handle(Context context) {
    context.insert(
      new PrintThenInsertOrNextHandler("a",
        new PrintThenInsertOrNextHandler("a.1"),
        new PrintThenInsertOrNextHandler("a.2"),
      ),
      new PrintThenInsertOrNextHandler("b",
        new PrintThenInsertOrNextHandler("b.1",
          new PrintThenInsertOrNextHandler("b.1.1")
        ),
      ),
      new PrintThenInsertOrNextHandler("c")
    );
  }
}
```

This would write the following to `System.out`…

```
a
a.1
a.2
b
b.1
b.1.1
c
```

This demonstrates how the _next_ handler of the handler that inserts the handlers becomes the _next_ handler of the last of the inserted handlers.
You might need to read that sentence more than once.

You should be able to see a certain nesting capability emerge.
This is important for composibility, and also for scoping which will be important when considering the registry context later in the chapter.

It would be natural at this point to think that it looks like a lot of work to build a handler structure for a typical web application
(i.e. one that dispatches requests matching certain request paths to endpoints).
Read on.

## Building handler chains

TODO
