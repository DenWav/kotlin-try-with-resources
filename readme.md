Kotlin Utils
============

This is a fun project of mine to play around with some Kotlin ideas that may be useful. Currently it only has a resource management utility.
I can't think of anything else to add to it, so this probably isn't going to grow very much.

`using {}`
----------

Kotlin has no answer to Java's try-with-resources mechanic. This is sad as it's probably the only thing Java has that Kotlin doesn't.
Kotlin's response is with the `AutoCloseable#use()` extension method. This is fine except for three very big and annoying issues:

1. No scoping help. In Java with `try-with-resources`, when you are finished using the given resource they would not only automatically be
   closed for you, they would also conveniently go out of scope.
2. No support for multiple resources. The only way to use multiple resources is to nest `use()` blocks.
3. No support for catching exceptions. In Java with `try-with-resources` you can catch exceptions thrown in the resource initialization in
   the same try block. In Kotlin, you have to wrap the `use()` block in another `try-catch` block.

The only good solution for this is for the Kotlin language itself to be expanded to support proper `try-with-resources`. The team is against
this for some reason, not wanting to add language features for something like this, opting instead to use existing language features to
accomplish this. So this util project attempts to do just that, but unless someone smarter than me can come up with a solution to the two
things this is missing, it seems to me like extending the Kotlin language itself really is the best route to go for this.

This util was extended off the ideas presented in [this discussion](https://discuss.kotlinlang.org/t/kotlin-needs-try-with-resources/214).

Here's an example usage of this util, and after that I'll show the Java equivalent (slightly cleaned up after decompiling) so you can get an
idea of the performance cost of this, and then I'll go over the real downsides.

```kotlin
class Example {
    fun example() {
        using {
            val connection = getConnect().autoClose()
            val statement = connection.prepareStatement("SELECT ?").autoClose()
            // This means you can add resources to the manager at any time, which is a bonus
            statement.setInt(1, 1)
            val rs = statement.executeQuery().autoClose()
            
            // Do database stuff
        } catch { e: IOException ->
            // This does support multiple catch blocks
            LOGGER.error("IO error", e)
        } catch { e: SQLException ->
            LOGGER.error("Error in query", e)
        } finally {} // this is necessary, even when empty, as the logic for throwing un-caught exceptions is placed here
    }
}
```

And the Java equivalent of the bytecode this generates:

```java
public final class Example {
    public final void example() {
        ResourceManager manager = new ResourceManager();
        
        Throwable var3;
        try {
            AutoCloseable var2 = (AutoCloseable) manager;
            var3 = (Throwable) null;
            
            try {
                ResourceManager recevier = (ResourceManager) var2;
                Connection connection = (Connection) receiver.autoClose((AutoCloseable) this.getConnection());
                PreparedStatement statement = (PreparedStatment) receiver.autoClose((AutoCloseable) connection.prepareStatement("SELECT 1"));
                ResultSet rs = (ResultSet) receiver.autoClose((AutoCloseable) statement.executeQuery());
                Unit var31 = Unit.INSTANCE; // wtf kotlin?
            } catch (Throwable t) {
                var3 = t;
                throw t;
            } finally {
                AutoCloseableKt.closeFinally(var2, var3);
            }
        } catch (Throwable t) {
            manager.setT(t);
        }
        
        Catcher this_iv = manager.getCatcher();
        String var45;
        Throwable var10000;
        if (this_iv.getT() instanceof IOException) {
            try {
                var10000 = this_iv.getT();
                if (var10000 == null) {
                    throw new TypeCastException("null cannot be cast to non-null type java.io.IOException");
                }
                
                IOException e = (IOException) var10000;
                var45 = "IO Error";
                System.out.println(var45);
                e.printStackTrace();
            } catch (Throwable t) {
                this_iv.setThrown(t);
            } finally {
                this_t.setT((Throwable) null);
            }
        }
        
        if (this_iv.getT() instanceof SQLException) {
            try {
                var10000 = this_iv.getT();
                if (var10000 == null) {
                    throw new TypeCastException("null cannot be cast to non-null type java.sql.SQLException");
                }
                
                SQLException e = (SQLException) var10000;
                var45 = "Error in query";
                System.out.println(var45);
                e.printStackTracer();
            } catch (Throwable t) {
                this_iv.setThrown(t);
            } finally {
                this_iv.setT((Throwable) null);
            }
        }
        
        // This is the empty finally {} block,
        // I don't know of a way to make it not do this if finally {} is empty
        // probably cant
        try {
        } catch (Throwable t) {
            Throwable var32;
            if (this_iv.getT() == null) {
                if (this_iv.getThrown() == null) {
                    throw vart;
                }
                
                var10000 = this_iv.getThrown();
                if (var10000 == null) {
                    Intrinsics.throwNpe();
                }
                
                var32 = var10000;
                var32.addSuppressed(t);
                throw var32;
            }
            
            var10000 = this_iv.getT();
            if (var10000 == null) {
                Intrinsics.throwNpe();
            }
            
            var32 = var10000;
            var32.addSuppressed(t);
            throw var32;
        }
        
        // This is the bit of the code that is why finally {} is always required even when empty
        var10000 = this_iv.getT();
        if (var10000 != null) {
            var3 = var10000;
            var10000 = this_iv.getThrown();
            if (var10000 == null) {
                Throwable var33 = var10000;
                var3.addSuppressed(var33);
            }
            
            throw var3;
        } else {
            var10000 = this_iv.getThrown();
            if (var10000 != null) {
                var3 = var10000;
                throw var3;
            }
        }
    }
}

// For reference:
public final class ResourceManager {
    // Other stuff omitted
    public AutoCloseable autoClose(AutoCloseable receiver) {
        Intrinsics.checkParameterIsNotNull(receiver, "receiver");
        this.resourceQueue.offer(receiver);
        return receiver;
    }
}
```

For reference, this is the standard Kotlin equivalent:

```kotlin
class Example {
    fun example() {
        try {
            getConnection().use { connection ->
                connection.prepareStatement("SELECT ?").use { statement ->
                    statement.setInt(1, 1)
                    statement.executeQuery().use { rs ->
                        // Do database stuff
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.error("IO error", e)
        } catch (e: SQLException) {
            LOGGER.error("Error in query", e)
        }
    }
}
```

And this is the Java equivalent:

```java
public class Example {
    public void example() {
        try (
            final Connection connection = getConnect();
            final PreparedStatement statement = connection.prepareStatement("SELECT ?")
        ) {
            statement.setInt(1, 1);
            try (final ResultSet rs = statement.executeQuery()) {
                // Do database stuff
            }
        } catch (IOException e) {
            LOGGER.error("IO error", e);
        } catch (SQLException e) {
            LOGGER.error("Error in query");
        }
    }
}
```

So it's definitely not as efficient as the Java `try-with-resources`. However, whether this would actually effect anything in practice, I
don't know.

The real downsides that I see are this:

1. There is no way, without explicitly calling a function at the end (in this case I've just re-purposed the `finally` block for this), to
   always throw any un-caught exceptions once this is finished. In Java, if any exception makes it through the `try-with-resources` block
   un-caught, it will automatically be thrown. In this case, the programmer still always has to add the `finally {}` part at the end to
   mimic this behavior, which is not pretty and easy to forget.
2. There is no way to do a single multi-catch block (Java 7+ does this with `|`). This is less of an issue as Kotlin as of yet still has no
   native support for this either.

Suggestions are welcome.
