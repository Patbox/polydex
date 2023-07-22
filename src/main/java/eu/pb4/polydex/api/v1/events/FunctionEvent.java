package eu.pb4.polydex.api.v1.events;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class FunctionEvent<T, R> {
    private List<T> handlers = new ArrayList<>();

    public void register(T listener) {
        this.handlers.add(listener);
    }

    public R invoke(Function<Collection<T>, R> invoker) {
        return invoker.apply(handlers);
    }
}
