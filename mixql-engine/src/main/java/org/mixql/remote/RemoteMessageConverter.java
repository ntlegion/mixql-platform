package org.mixql.remote;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mixql.remote.messages.*;
import org.mixql.remote.messages.module.*;
import org.mixql.remote.messages.gtype.*;
import org.mixql.remote.messages.module.Error;
import org.mixql.remote.messages.module.worker.*;

import java.nio.charset.StandardCharsets;
import java.util.*;


public class RemoteMessageConverter {
    public static Message unpackAnyMsgFromArray(byte[] array) {
        return unpackAnyMsg(new String(array, StandardCharsets.UTF_8));
    }

    private static String[] parseStringsArray(JSONArray jsonArrObject) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < jsonArrObject.size(); i++) {
            list.add(
                    (String) jsonArrObject.get(i)
            );
        }
        String[] arr = new String[list.size()];
        return list.toArray(arr);
    }

    private static Message[] parseMessagesArray(JSONArray jsonArrObject) throws Exception {
        ArrayList<Message> list = new ArrayList();
        for (int i = 0; i < jsonArrObject.size(); i++) {
            list.add(
                    _unpackAnyMsg((JSONObject) jsonArrObject.get(i))
            );
        }
        Message[] arr = new Message[list.size()];
        return list.toArray(arr);
    }

    private static Message _unpackAnyMsg(JSONObject anyMsgJsonObject) throws Exception {
        switch ((String) anyMsgJsonObject.get("type")) {
            case "org.mixql.remote.messages.module.EngineName":
                return new EngineName(
                        (String) anyMsgJsonObject.get("name")
                );
            case "org.mixql.remote.messages.module.ShutDown":
                return new ShutDown();
            case "org.mixql.remote.messages.module.Execute":
                return new Execute(
                        (String) anyMsgJsonObject.get("statement")
                );
            case "org.mixql.remote.messages.module.Param":
                return new Param(
                        (String) anyMsgJsonObject.get("name"),
                        _unpackAnyMsg((JSONObject) anyMsgJsonObject.get("msg"))
                );
            case "org.mixql.remote.messages.module.Error":
                return new Error(
                        "error while unpacking from json Error: " + anyMsgJsonObject.get("msg")
                );
            case "org.mixql.remote.messages.module.ExecuteFunction":
                return new ExecuteFunction(
                        (String) anyMsgJsonObject.get("name"),
                        parseMessagesArray((JSONArray) anyMsgJsonObject
                                .get("params")
                        )
                );
            case "org.mixql.remote.messages.module.GetDefinedFunctions":
                return new GetDefinedFunctions();
            case "org.mixql.remote.messages.module.DefinedFunctions":
                return new DefinedFunctions(
                        parseStringsArray((JSONArray) anyMsgJsonObject.get("arr"))
                );
            case "org.mixql.remote.messages.gtype.NULL":
                return new NULL();
            case "org.mixql.remote.messages.gtype.Bool":
                return new Bool(
                        Boolean.parseBoolean((String) anyMsgJsonObject.get("value"))
                );
            case "org.mixql.remote.messages.gtype.gInt":
                return new gInt(
                        Integer.parseInt((String) anyMsgJsonObject.get("value"))
                );
            case "org.mixql.remote.messages.gtype.gDouble":
                return new gDouble(
                        Double.parseDouble((String) anyMsgJsonObject.get("value"))
                );
            case "org.mixql.remote.messages.gtype.gString":
                return new gString(
                        (String) anyMsgJsonObject.get("value"),
                        (String) anyMsgJsonObject.get("quote")
                );
            case "org.mixql.remote.messages.gtype.gArray":
                return new gArray(
                        parseMessagesArray((JSONArray) anyMsgJsonObject.get("arr"))
                );
            case "org.mixql.remote.messages.gtype.map":
                JSONArray mapJsonObject = (JSONArray) anyMsgJsonObject.get("map");
                Map<Message, Message> m = new HashMap<>();
                for (int i = 0; i < mapJsonObject.size(); i++) {
                    m.put(_unpackAnyMsg(
                                    (JSONObject) ((JSONObject) mapJsonObject.get(i)).get("key")
                            ),
                            _unpackAnyMsg(
                                    (JSONObject) ((JSONObject) mapJsonObject.get(i)).get("value")
                            )
                    );
                }
                return new map(m);
            case "org.mixql.remote.messages.module.worker.GetPlatformVar":
                return new GetPlatformVar(
                        (String) anyMsgJsonObject.get("sender"),
                        (String) anyMsgJsonObject.get("name"),
                        ((String) anyMsgJsonObject.get("clientAddress")).getBytes()
                );
            case "org.mixql.remote.messages.module.worker.GetPlatformVars":
                return new GetPlatformVars(
                        (String) anyMsgJsonObject.get("sender"),
                        parseStringsArray((JSONArray) anyMsgJsonObject.get("names")),
                        ((String) anyMsgJsonObject.get("clientAddress")).getBytes()
                );
            case "org.mixql.remote.messages.module.worker.GetPlatformVarsNames":
                return new GetPlatformVarsNames(
                        (String) anyMsgJsonObject.get("sender"),
                        ((String) anyMsgJsonObject.get("clientAddress")).getBytes()
                );
            case "org.mixql.remote.messages.module.worker.PlatformVar":
                return new PlatformVar(
                        (String) anyMsgJsonObject.get("sender"),
                        (String) anyMsgJsonObject.get("name"),
                        _unpackAnyMsg((JSONObject) anyMsgJsonObject.get("msg"))
                );
            case "org.mixql.remote.messages.module.worker.PlatformVars":
                return new PlatformVars(
                        (String) anyMsgJsonObject.get("sender"),
                        (Param[]) parseMessagesArray((JSONArray) anyMsgJsonObject
                                .get("vars")
                        )
                );
            case "org.mixql.remote.messages.module.worker.PlatformVarsNames":
                return new PlatformVarsNames(
                        (String) anyMsgJsonObject.get("sender"),
                        parseStringsArray((JSONArray) anyMsgJsonObject.get("names"))
                );
            case "org.mixql.remote.messages.module.worker.PlatformVarsWereSet":
                return new PlatformVarsWereSet(
                        (String) anyMsgJsonObject.get("sender"),
                        new ArrayList<String>(
                                Arrays.asList(parseStringsArray((JSONArray) anyMsgJsonObject.get("names")))
                        )
                );
            case "org.mixql.remote.messages.module.worker.PlatformVarWasSet":
                return new PlatformVarWasSet(
                        (String) anyMsgJsonObject.get("sender"),
                        (String) anyMsgJsonObject.get("name")
                );
            case "org.mixql.remote.messages.module.worker.SendMsgToPlatform":
                return new SendMsgToPlatform(
                        ((String) anyMsgJsonObject.get("clientAddress")).getBytes(),
                        _unpackAnyMsg((JSONObject) anyMsgJsonObject.get("msg")),
                        (String) anyMsgJsonObject.get("sender")
                );
            case "org.mixql.remote.messages.module.worker.SetPlatformVar":
                return new SetPlatformVar(
                        (String) anyMsgJsonObject.get("sender"),
                        (String) anyMsgJsonObject.get("name"),
                        _unpackAnyMsg((JSONObject) anyMsgJsonObject.get("msg")),
                        ((String) anyMsgJsonObject.get("clientAddress")).getBytes()
                );
            case "org.mixql.remote.messages.module.worker.SetPlatformVars":
                JSONArray varsJsonObject = (JSONArray) anyMsgJsonObject.get("vars");
                Map<String, Message> varsMap = new HashMap<>();
                for (int i = 0; i < varsJsonObject.size(); i++) {
                    varsMap.put(
                            (String) anyMsgJsonObject.get("key"),
                            _unpackAnyMsg(
                                    (JSONObject) ((JSONObject) varsJsonObject.get(i)).get("value")
                            )
                    );
                }
                return new SetPlatformVars(
                        (String) anyMsgJsonObject.get("sender"),
                        varsMap,
                        ((String) anyMsgJsonObject.get("clientAddress")).getBytes()
                );
            case "org.mixql.remote.messages.module.worker.WorkerFinished":
                return new WorkerFinished(
                        (String) anyMsgJsonObject.get("sender")
                );
        }
        throw new Exception("_unpackAnyMsg: unknown anyMsgJsonObject" + anyMsgJsonObject);
    }

    public static Message unpackAnyMsg(String json) {

        try {
            JSONObject anyMsgJsonObject = (JSONObject) JSONValue.parseWithException(json);
            return _unpackAnyMsg(anyMsgJsonObject);
        } catch (Exception e) {
            return new Error(
                    String.format(
                            "Protobuf anymsg converter: Error: %s", e.getMessage()
                    )
            );
        }
    }

    public static byte[] toArray(Message msg) throws Exception {
        return toJson(msg).getBytes(StandardCharsets.UTF_8);
    }

    private static JSONObject[] _toJsonObjects(Message[] msgs) throws Exception {
        ArrayList<JSONObject> list = new ArrayList<>();
        for (Message msg : msgs) {
            list.add(
                    _toJsonObject(msg)
            );
        }
        JSONObject[] arr = new JSONObject[list.size()];
        return list.toArray(arr);
    }

    private static JSONObject _toJsonObject(Message msg) throws Exception {
        if (msg instanceof EngineName) {
            return JsonUtils.buildEngineName(msg.type(), ((EngineName) msg).name);
        }

        if (msg instanceof ShutDown) {
            return JsonUtils.buildShutDown(msg.type());
        }

        if (msg instanceof Execute) {
            return JsonUtils.buildExecute(msg.type(), ((Execute) msg).statement);
        }

        if (msg instanceof Param) {
            return JsonUtils.buildParam(msg.type(), ((Param) msg).name, _toJsonObject(((Param) msg).msg));
        }

        if (msg instanceof Error) {
            return JsonUtils.buildError(msg.type(), ((Error) msg).msg);
        }

        if (msg instanceof ExecuteFunction) {
            return JsonUtils.buildExecuteFunction(msg.type(), ((ExecuteFunction) msg).name,
                    _toJsonObjects(((ExecuteFunction) msg).params)
            );
        }

        if (msg instanceof GetDefinedFunctions) {
            return JsonUtils.buildGetDefinedFunctions(msg.type());
        }


        if (msg instanceof DefinedFunctions) {
            return JsonUtils.buildDefinedFunction(msg.type(), ((DefinedFunctions) msg).arr);
        }

        if (msg instanceof NULL) {
            return JsonUtils.buildNULL(msg.type());
        }

        if (msg instanceof Bool) {
            return JsonUtils.buildBool(msg.type(), ((Bool) msg).value);
        }

        if (msg instanceof gInt) {
            return JsonUtils.buildInt(msg.type(), ((gInt) msg).value);
        }

        if (msg instanceof gDouble) {
            return JsonUtils.buildDouble(msg.type(), ((gDouble) msg).value);
        }

        if (msg instanceof gString) {
            return JsonUtils.buildGString(msg.type(), ((gString) msg).value, ((gString) msg).quote);
        }

        if (msg instanceof gArray) {
            return JsonUtils.buildGArray(msg.type(), _toJsonObjects(((gArray) msg).arr));
        }

        if (msg instanceof map) {
            Set<Message> keys = ((map) msg).getMap().keySet();
            Collection<Message> values = ((map) msg).getMap().values();
            return JsonUtils.buildMap(msg.type(), _toJsonObjects(keys.toArray(new Message[keys.size()])),
                    _toJsonObjects(values.toArray(new Message[values.size()])));
        }

        if (msg instanceof GetPlatformVar) {
            GetPlatformVar msgVar = ((GetPlatformVar) msg);
            return JsonUtils.buildGetPlatformVar(msg.type(), msgVar.name, msgVar.sender(),
                    new String(msgVar.clientAddress()));
        }

        if (msg instanceof GetPlatformVars) {
            GetPlatformVars msgVars = ((GetPlatformVars) msg);
            return JsonUtils.buildGetPlatformVars(msg.type(), msgVars.names, msgVars.sender(),
                    new String(msgVars.clientAddress()));
        }

        if (msg instanceof GetPlatformVarsNames) {
            GetPlatformVarsNames msgTmp = ((GetPlatformVarsNames) msg);
            return JsonUtils.buildGetPlatformVarsNames(msgTmp.type(), msgTmp.sender(),
                    new String(msgTmp.clientAddress()));
        }

        if (msg instanceof PlatformVar) {
            PlatformVar msgTmp = ((PlatformVar) msg);
            return JsonUtils.buildPlatformVar(msgTmp.type(), msgTmp.sender(),
                    msgTmp.name, _toJsonObject(msgTmp.msg));
        }

        if (msg instanceof PlatformVars) {
            PlatformVars msgTmp = ((PlatformVars) msg);
            return JsonUtils.buildPlatformVars(msgTmp.type(), msgTmp.sender(),
                    _toJsonObjects(msgTmp.vars));
        }

        if (msg instanceof PlatformVarsNames) {
            PlatformVarsNames msgTmp = ((PlatformVarsNames) msg);
            return JsonUtils.buildPlatformVarsNames(msgTmp.type(), msgTmp.names, msgTmp.sender());
        }

        if (msg instanceof PlatformVarsWereSet) {
            PlatformVarsWereSet msgTmp = ((PlatformVarsWereSet) msg);
            return JsonUtils.buildPlatformVarsWereSet(msgTmp.type(), msgTmp.names.toArray(new String[0]),
                    msgTmp.sender());
        }

        if (msg instanceof PlatformVarWasSet) {
            PlatformVarWasSet msgTmp = ((PlatformVarWasSet) msg);
            return JsonUtils.buildPlatformVarWasSet(msgTmp.type(), msgTmp.name,
                    msgTmp.sender());
        }

        if (msg instanceof SendMsgToPlatform) {
            SendMsgToPlatform msgTmp = ((SendMsgToPlatform) msg);
            return JsonUtils.buildSendMsgToPlatform(msgTmp.type(),
                    msgTmp.sender(),
                    new String(msgTmp.clientAddress()),
                    _toJsonObject(msgTmp.msg)
            );
        }

        if (msg instanceof SetPlatformVar) {
            SetPlatformVar msgTmp = ((SetPlatformVar) msg);
            return JsonUtils.buildSetPlatformVar(msgTmp.type(),
                    msgTmp.sender(),
                    msgTmp.name,
                    _toJsonObject(msgTmp.msg)
            );
        }

        if (msg instanceof WorkerFinished) {
            WorkerFinished msgTmp = ((WorkerFinished) msg);
            return JsonUtils.buildWorkerFinished(msgTmp.type(),
                    msgTmp.sender()
            );
        }

        if (msg instanceof SetPlatformVars) {
            SetPlatformVars msgTmp = ((SetPlatformVars) msg);

            return JsonUtils.buildSetPlatformVars(msgTmp.type(),
                    msgTmp.sender(),
                    new String(msgTmp.clientAddress()),
                    msgTmp.vars.keySet().toArray(new String[msgTmp.vars.keySet().size()]),
                    _toJsonObjects(
                            msgTmp.vars.values().toArray(
                                    new Message[msgTmp.vars.values().size()]
                            )
                    )
            );
        }

        throw new Exception("_toJsonObject Error. Unknown type of message " + msg);
    }

    public static String toJson(Message msg) throws Exception {
        return _toJsonObject(msg).toJSONString();
    }
}
