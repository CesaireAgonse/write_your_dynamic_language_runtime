package fr.umlv.smalljs.jvminterp;

import static fr.umlv.smalljs.rt.JSObject.UNDEFINED;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.invoker;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import fr.umlv.smalljs.rt.ArrayMap;
import fr.umlv.smalljs.rt.ArrayMap.Layout;
import fr.umlv.smalljs.rt.Failure;
import fr.umlv.smalljs.rt.JSObject;

public class RT {
  private static final MethodHandle INVOKER, LOOKUP, REGISTER, TRUTH, GET_MH, METH_LOOKUP_MH;
  static {
    var lookup = MethodHandles.lookup();
    try {
      INVOKER = lookup.findVirtual(JSObject.class, "invoke", methodType(Object.class, Object.class, Object[].class));
      LOOKUP = lookup.findVirtual(JSObject.class, "lookup", methodType(Object.class, String.class));
      REGISTER = lookup.findVirtual(JSObject.class, "register", methodType(void.class, String.class, Object.class));
      TRUTH = lookup.findStatic(RT.class, "truth", methodType(boolean.class, Object.class));

      GET_MH = lookup.findVirtual(JSObject.class, "getMethodHandle", methodType(MethodHandle.class));
      METH_LOOKUP_MH = lookup.findStatic(RT.class, "lookupMethodHandle", methodType(MethodHandle.class, JSObject.class, String.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  public static Object bsm_undefined(Lookup lookup, String name, Class<?> type) {
    return UNDEFINED;
  }

  public static Object bsm_const(Lookup lookup, String name, Class<?> type, int constant) {
    return constant;
  }

  public static CallSite bsm_funcall(Lookup lookup, String name, MethodType type) {
    // throw new UnsupportedOperationException("TODO bsm_funcall");
    // take GET_MH method handle
    MethodHandle combiner = GET_MH;
    // make it accept an Object (not a JSObject) as first parameter
    combiner = combiner.asType(MethodType.methodType(MethodHandle.class, Object.class));
    // create a generic invoker (MethodHandles.invoker()) on the parameter types without the qualifier
    //exact est plus rapide alors que l'autre fait du doxing mais ne gère pas les '...'
    var invoker = MethodHandles.invoker(type.dropParameterTypes(0,1));
    // drop the qualifier
    invoker = MethodHandles.dropArguments(invoker, 1, Object.class);
    // use MethopdHandles.foldArguments with GET_MH as combiner
    var target = MethodHandles.foldArguments(invoker, combiner);
    // create a constant callsite
    return new ConstantCallSite(target);
  }

  public static CallSite bsm_lookup(Lookup lookup, String name, MethodType type, String functionName) {
    //throw new UnsupportedOperationException("TODO bsm_lookup");
    var classLoader = (FunClassLoader) lookup.lookupClass().getClassLoader();
    var globalEnv = classLoader.getGlobal();
    // get the LOOKUP method handle
    MethodHandle target = LOOKUP;
    // use the global environment as first argument and the functionName as second argument
    target = MethodHandles.insertArguments(target, 0, globalEnv, functionName);
    // create a constant callsite
    return new ConstantCallSite(target);
  }

  public static Object bsm_fun(Lookup lookup, String name, Class<?> type, int funId) {
    throw new UnsupportedOperationException("TODO bsm_fun");
    //var classLoader = (FunClassLoader) lookup.lookupClass().getClassLoader();
    //var globalEnv = classLoader.getGlobal();
    //var fun = classLoader.getDictionary().lookupAndClear(funId);
    //return ByteCodeRewriter.createFunction(fun.name().orElse("lambda"), fun.parameters(), fun.body(), globalEnv);
  }

  public static CallSite bsm_register(Lookup lookup, String name, MethodType type, String functionName) {
    throw new UnsupportedOperationException("TODO bsm_register");
    //var classLoader = (FunClassLoader) lookup.lookupClass().getClassLoader();
    //var globalEnv = classLoader.getGlobal();
    //get the REGISTER method handle
    // use the global environment as first argument and the functionName as second argument
    // create a constant callsite
  }

  @SuppressWarnings("unused")  // used by a method handle
  private static boolean truth(Object o) {
    return o != null && o != UNDEFINED && o != Boolean.FALSE;
  }
  public static CallSite bsm_truth(Lookup lookup, String name, MethodType type) {
    throw new UnsupportedOperationException("TODO bsm_truth");
    // get the TRUTH method handle
    // create a constant callsite
  }

  public static CallSite bsm_get(Lookup lookup, String name, MethodType type, String fieldName) {
    throw new UnsupportedOperationException("TODO bsm_get");
    // get the LOOKUP method handle
    // use the fieldName as second argument
    // make it accept an Object (not a JSObject) as first parameter
    // create a constant callsite
  }

  public static CallSite bsm_set(Lookup lookup, String name, MethodType type, String fieldName) {
    throw new UnsupportedOperationException("TODO bsm_set");
    // get the REGISTER method handle
    // use the fieldName as second argument
    // make it accept an Object (not a JSObject) as first parameter
    // create a constant callsite
  }

  @SuppressWarnings("unused")  // used by a method handle
  private static MethodHandle lookupMethodHandle(JSObject receiver, String fieldName) {
    var function = (JSObject) receiver.lookup(fieldName);
    return function.getMethodHandle();
  }

  public static CallSite bsm_methodcall(Lookup lookup, String name, MethodType type) {
    throw new UnsupportedOperationException("TODO bsm_methodcall");
    //var combiner = insertArguments(METH_LOOKUP_MH, 1, name).asType(methodType(MethodHandle.class, Object.class));
    //var target = foldArguments(invoker(type), combiner);
    //return new ConstantCallSite(target);
  }
}
