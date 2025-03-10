package fr.umlv.smalljs.astinterp;

import fr.umlv.smalljs.ast.Expr;
import fr.umlv.smalljs.ast.Expr.Block;
import fr.umlv.smalljs.ast.Expr.FieldAccess;
import fr.umlv.smalljs.ast.Expr.FieldAssignment;
import fr.umlv.smalljs.ast.Expr.Fun;
import fr.umlv.smalljs.ast.Expr.FunCall;
import fr.umlv.smalljs.ast.Expr.If;
import fr.umlv.smalljs.ast.Expr.Literal;
import fr.umlv.smalljs.ast.Expr.LocalVarAccess;
import fr.umlv.smalljs.ast.Expr.LocalVarAssignment;
import fr.umlv.smalljs.ast.Expr.MethodCall;
import fr.umlv.smalljs.ast.Expr.New;
import fr.umlv.smalljs.ast.Expr.Return;
import fr.umlv.smalljs.ast.Script;
import fr.umlv.smalljs.rt.Failure;
import fr.umlv.smalljs.rt.JSObject;
import fr.umlv.smalljs.rt.JSObject.Invoker;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

import static fr.umlv.smalljs.rt.JSObject.UNDEFINED;
import static java.util.stream.Collectors.joining;

public class ASTInterpreter {
  private static JSObject asJSObject(Object value, int lineNumber) {
    if (!(value instanceof JSObject jsObject)) {
      throw new Failure("at line " + lineNumber + ", type error " + value + " is not a JSObject");
    }
    return jsObject;
  }

  static Object visit(Expr expression, JSObject env) {
    return switch (expression) {
      case Block(List<Expr> instrs, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO Block");
        // TODO loop over all instructions
        for ( var instr : instrs){
          visit(instr, env);
        }
        yield UNDEFINED;
      }
      case Literal<?>(Object value, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO Literal");
        yield value;
      }
      case FunCall(Expr qualifier, List<Expr> args, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO FunCall");
        var maybeFunction = visit(qualifier,env);
        if (!(maybeFunction instanceof JSObject jsObject)) {
          throw new Failure( "not a function " + maybeFunction + " at " + lineNumber);
        }
        var  values = args.stream().map(arg -> visit(arg, env)).toArray();
        yield jsObject.invoke(UNDEFINED,values);
      }
      case LocalVarAccess(String name, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO LocalVarAccess");
        /*
        var valueOrUndefined = env.lookup(name);
        if (valueOrUndefined == UNDEFINED){
          throw new Failure("variable " + name + " is undefined at " + lineNumber);
        }
        yield valueOrUndefined;
        */
        yield env.lookup(name);
      }
      case LocalVarAssignment(String name, Expr expr, boolean declaration, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO LocalVarAssignment");

        var result = visit(expr, env);
        var valueOrUndefined = env.lookup(name);
        if (declaration && (valueOrUndefined != UNDEFINED)){
          throw new Failure("variable " + name + " is already assigned at line " + lineNumber);
        }
        env.register(name, result);
        yield result;
      }
      case Fun(Optional<String> optName, List<String> parameters, Block body, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO Fun");
        var functionName = optName.orElse("lambda");
        Invoker invoker = new Invoker() {
          @Override
          public Object invoke(JSObject self, Object receiver, Object... args) {
            // check the arguments length
            if (args.length != parameters.size()){
              throw new Failure("wrong number of arguments" + args.length +
                      " (should be " + parameters.size() + ") at line " + lineNumber);
            }
            // create a new environment
            var newEnv = JSObject.newEnv(env);
            // add this and all the parameters
            newEnv.register("this", receiver);
            for (var parameter : parameters){

            }
            for (var i = 0; i < parameters.size(); i++){
              var parameter = parameters.get(i);
              newEnv.register(parameter, args[i]);
            }
            // visit the body
            try {
              return visit(body, newEnv);
            } catch (ReturnError error) {
              return error.getValue();
            }
          }
        };
        // create the JS function with the invoker
        var function = JSObject.newFunction(functionName, invoker);
        // register it if necessary
        optName.ifPresent(name -> {
          env.register(name, function);
        });
        // yield the function
        yield function;
      }
      case Return(Expr expr, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO Return");
        throw new ReturnError(visit(expr, env));

      }
      case If(Expr condition, Block trueBlock, Block falseBlock, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO If");
        var value = visit(condition, env);
        if ( value instanceof Integer i && i == 0){
          yield visit(falseBlock, env);
        } else {
          yield visit(trueBlock, env);
        }

      }
      case New(Map<String, Expr> initMap, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO New");
        var Mapfields = new HashMap<String, Object>();

        initMap.forEach((name, expr) -> {Mapfields.put(name, visit(expr, env));});


        yield new Object(){

          public final HashMap<String, Object> fields = Mapfields;

          @Override
          public String toString(){
            var builder = new StringBuilder();
            builder.append("{ // object\n");
            fields.forEach((name, expr) -> {
              //builder.append("  ").append(name).append(": ").append(visit((Expr) expr, env)).append("\n");
              builder.append("  ").append(name).append(": ").append(expr).append("\n");
            });
            builder.append("  proto: null\n");
            builder.append("}");
            return builder.toString();
          }
        };
      }
      case FieldAccess(Expr receiver, String name, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO FieldAccess");
        var obj = visit(receiver, env);
          try {
              var champs = (HashMap<String, Object>) obj.getClass().getDeclaredField("fields").get(obj);
              yield champs.getOrDefault(name, "undefined");
          } catch (NoSuchFieldException e) {
              throw new RuntimeException(e);
          } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
          }

      }
      case FieldAssignment(Expr receiver, String name, Expr expr, int lineNumber) -> {
        //throw new UnsupportedOperationException("TODO FieldAssignment");
        yield null;
      }
      case MethodCall(Expr receiver, String name, List<Expr> args, int lineNumber) -> {
        throw new UnsupportedOperationException("TODO MethodCall");
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static void interpret(Script script, PrintStream outStream) {
    JSObject globalEnv = JSObject.newEnv(null);
    Block body = script.body();
    globalEnv.register("global", globalEnv);
    globalEnv.register("print", JSObject.newFunction("print", (self, receiver, args) -> {
      System.err.println("print called with " + Arrays.toString(args));
      outStream.println(Arrays.stream(args).map(Object::toString).collect(joining(" ")));
      return UNDEFINED;
    }));
    globalEnv.register("+", JSObject.newFunction("+", (self, receiver, args) -> (Integer) args[0] + (Integer) args[1]));
    globalEnv.register("-", JSObject.newFunction("-", (self, receiver, args) -> (Integer) args[0] - (Integer) args[1]));
    globalEnv.register("/", JSObject.newFunction("/", (self, receiver, args) -> (Integer) args[0] / (Integer) args[1]));
    globalEnv.register("*", JSObject.newFunction("*", (self, receiver, args) -> (Integer) args[0] * (Integer) args[1]));
    globalEnv.register("%", JSObject.newFunction("%", (self, receiver, args) -> (Integer) args[0] % (Integer) args[1]));

    globalEnv.register("==", JSObject.newFunction("==", (self, receiver, args) -> args[0].equals(args[1]) ? 1 : 0));
    globalEnv.register("!=", JSObject.newFunction("!=", (self, receiver, args) -> !args[0].equals(args[1]) ? 1 : 0));
    globalEnv.register("<", JSObject.newFunction("<", (self, receiver, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) < 0) ? 1 : 0));
    globalEnv.register("<=", JSObject.newFunction("<=", (self, receiver, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) <= 0) ? 1 : 0));
    globalEnv.register(">", JSObject.newFunction(">", (self, receiver, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) > 0) ? 1 : 0));
    globalEnv.register(">=", JSObject.newFunction(">=", (self, receiver, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) >= 0) ? 1 : 0));
    visit(body, globalEnv);
  }
}

