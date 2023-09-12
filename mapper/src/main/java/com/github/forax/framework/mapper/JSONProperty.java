package com.github.forax.framework.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)/*Moment ou c'est utilisé*/
@Target({METHOD, RECORD_COMPONENT}) /*cible potentielles*/
public @interface JSONProperty {
  String value();
}
