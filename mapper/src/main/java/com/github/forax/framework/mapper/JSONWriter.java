package com.github.forax.framework.mapper;

import java.beans.PropertyDescriptor;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

public final class JSONWriter {
  @FunctionalInterface
  private interface Generator {
    String generate(JSONWriter writer, Object bean);
  }

  private static final ClassValue<List<Generator>> CACHE = new ClassValue<>() {
    @Override
    protected List<Generator> computeValue(Class<?> type) {
      return Arrays.stream(Utils.beanInfo(type)
        .getPropertyDescriptors())
        .filter(property -> !property.getName().equals("class"))
        .<Generator>map(property -> {
          var key = "\"" + property.getName()+ "\": ";
          var readMethod = property.getReadMethod();
          return (writer, bean) -> key  + writer.toJSON(Utils.invokeMethod(bean, readMethod));
        })
        .toList();
    }
  };


  public String toJSON(Object o) {
    return switch (o) {
      case null -> "null";
      case String s -> '"' + s + '"';
      case Boolean b -> b.toString();
      case Integer i -> i.toString();
      case Double d -> d.toString();
      default -> objectToJSON(o);
    };
  }


  private String objectToJSON(Object o) {

    return CACHE.get(o.getClass()).stream()
      .map(generator -> generator.generate(this, o))
      .collect(joining(", ", "{","}"));

  }
}
