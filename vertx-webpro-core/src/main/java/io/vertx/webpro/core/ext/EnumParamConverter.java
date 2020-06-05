package io.vertx.webpro.core.ext;

public interface EnumParamConverter<E, T> {

    E convertToEnum(T t);

}
