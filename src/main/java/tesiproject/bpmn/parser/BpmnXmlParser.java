package tesiproject.bpmn.parser;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tesiproject.bpmn.model.*;
import tesiproject.bpmn.model.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.*;

@Component
public class BpmnXmlParser {

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    public ProcessModel parse(InputStream xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            Document doc = dbf.newDocumentBuilder().parse(xml);
            XPath xp = newXPath();

            ProcessModel model = new ProcessModel();
            model.setId(readOptionalAttr(xp, doc, "/bpmn:definitions/@id"));
            model.setNamespace(readOptionalAttr(xp, doc, "/bpmn:definitions/@targetNamespace"));
            model.setName(readOptionalAttr(xp, doc, "/bpmn:definitions/@name")); // spesso vuoto

            // 1) Participants (collaboration -> participant)
            parseParticipants(xp, doc, model);

            // 2) Lanes (laneSet -> lane -> flowNodeRef)  <-- IMPORTANTE: serve al VariableExtractor
            parseLanes(xp, doc, model);

            // 3) Nodes (tasks, gateways, events)
            parseNodes(xp, doc, model);

            // 4) Flows (sequenceFlow + messageFlow)
            parseFlows(xp, doc, model);

            // 5) Attach laneId to nodes where possible (flowNodeRef mapping)
            attachLaneIds(model);

            // 6) Build graph index for incoming/outgoing sequence flows
            model.buildIndex();

            return model;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid BPMN XML file", e);
        }
    }

    /* -------------------- parsing parts -------------------- */

    private void parseParticipants(XPath xp, Document doc, ProcessModel model) throws XPathExpressionException {
        NodeList participants = (NodeList) xp.evaluate("/bpmn:definitions/bpmn:collaboration/bpmn:participant", doc, XPathConstants.NODESET);

        for (int i = 0; i < participants.getLength(); i++) {
            Element p = (Element) participants.item(i);
            model.getParticipants().add(new Participant(p.getAttribute("id"), p.getAttribute("name"), p.getAttribute("processRef")));
        }
    }

    /*
     Popola model.getLanes() con id + name + flowNodeRef
     Questo è ciò che userà VariableExtractor per creare i ruoli umani (participants_<role>)
     */
    private void parseLanes(XPath xp, Document doc, ProcessModel model) throws XPathExpressionException {
        NodeList lanes = (NodeList) xp.evaluate("/bpmn:definitions//bpmn:laneSet/bpmn:lane", doc, XPathConstants.NODESET);

        // Evita duplicati se lo stesso id appare più volte
        Set<String> seenLaneIds = new HashSet<>();

        for (int i = 0; i < lanes.getLength(); i++) {
            Element laneEl = (Element) lanes.item(i);

            String laneId = emptyToNull(laneEl.getAttribute("id"));
            if (laneId == null) continue;
            if (!seenLaneIds.add(laneId)) continue;

            String laneName = emptyToNull(laneEl.getAttribute("name"));

            Lane lane = new Lane(laneId, laneName, null);

            NodeList refs = laneEl.getElementsByTagNameNS(BPMN_NS, "flowNodeRef");
            for (int r = 0; r < refs.getLength(); r++) {
                String nodeId = emptyToNull(refs.item(r).getTextContent());
                if (nodeId != null) lane.addNodeRef(nodeId);
            }

            model.getLanes().add(lane);
        }
    }

    private void parseNodes(XPath xp, Document doc, ProcessModel model) throws XPathExpressionException {
        // Task
        NodeList actNodes = (NodeList) xp.evaluate("/bpmn:definitions//bpmn:process//bpmn:task"
                        + " | /bpmn:definitions//bpmn:process//bpmn:userTask" + " | /bpmn:definitions//bpmn:process//bpmn:sendTask"
                        + " | /bpmn:definitions//bpmn:process//bpmn:receiveTask" + " | /bpmn:definitions//bpmn:process//bpmn:serviceTask"
                        + " | /bpmn:definitions//bpmn:process//bpmn:scriptTask" + " | /bpmn:definitions//bpmn:process//bpmn:manualTask"
                        + " | /bpmn:definitions//bpmn:process//bpmn:businessRuleTask" + " | /bpmn:definitions//bpmn:process//bpmn:callActivity"
                        + " | /bpmn:definitions//bpmn:process//bpmn:subProcess", doc, XPathConstants.NODESET);

        for (int i = 0; i < actNodes.getLength(); i++) {
            Element el = (Element) actNodes.item(i);
            String id = el.getAttribute("id");
            String name = el.getAttribute("name");
            ActivityType type = mapActivityType(el.getLocalName());
            model.getNodes().add(new Activity(id, name, null, type));
        }

        // Gateway
        NodeList gwNodes = (NodeList) xp.evaluate("/bpmn:definitions//bpmn:process//bpmn:exclusiveGateway"
                        + " | /bpmn:definitions//bpmn:process//bpmn:parallelGateway" + " | /bpmn:definitions//bpmn:process//bpmn:inclusiveGateway"
                        + " | /bpmn:definitions//bpmn:process//bpmn:eventBasedGateway" + " | /bpmn:definitions//bpmn:process//bpmn:complexGateway",
                        doc, XPathConstants.NODESET);

        for (int i = 0; i < gwNodes.getLength(); i++) {
            Element el = (Element) gwNodes.item(i);
            String id = el.getAttribute("id");
            String name = el.getAttribute("name");
            GatewayType type = mapGatewayType(el.getLocalName());
            model.getNodes().add(new Gateway(id, name, null, type));
        }

        // Eventi
        NodeList evNodes = (NodeList) xp.evaluate("/bpmn:definitions//bpmn:process//bpmn:startEvent"
                        + " | /bpmn:definitions//bpmn:process//bpmn:endEvent" + " | /bpmn:definitions//bpmn:process//bpmn:intermediateCatchEvent"
                        + " | /bpmn:definitions//bpmn:process//bpmn:intermediateThrowEvent", doc, XPathConstants.NODESET);

        for (int i = 0; i < evNodes.getLength(); i++) {
            Element el = (Element) evNodes.item(i);
            String id = el.getAttribute("id");
            String name = el.getAttribute("name");
            EventType type = mapEventType(el.getLocalName());
            model.getNodes().add(new Event(id, name, null, type));
        }
    }

    private void parseFlows(XPath xp, Document doc, ProcessModel model) throws XPathExpressionException {
        NodeList seqFlows = (NodeList) xp.evaluate("/bpmn:definitions//bpmn:process//bpmn:sequenceFlow", doc, XPathConstants.NODESET);

        for (int i = 0; i < seqFlows.getLength(); i++) {
            Element el = (Element) seqFlows.item(i);

            String id = el.getAttribute("id");
            String name = emptyToNull(el.getAttribute("name"));
            String sourceRef = el.getAttribute("sourceRef");
            String targetRef = el.getAttribute("targetRef");

            // ConditionExpression
            String condition = null;
            NodeList conds = el.getElementsByTagNameNS(BPMN_NS, "conditionExpression");
            if (conds.getLength() > 0) {
                condition = emptyToNull(conds.item(0).getTextContent());
            }

            // Se non c'è conditionExpression, si cerca la condizione nel name
            if (condition == null) condition = name;

            model.getFlows().add(new SequenceFlow(id, name, sourceRef, targetRef, condition));
        }

        NodeList msgFlows = (NodeList) xp.evaluate("/bpmn:definitions/bpmn:collaboration/bpmn:messageFlow", doc, XPathConstants.NODESET);

        for (int i = 0; i < msgFlows.getLength(); i++) {
            Element el = (Element) msgFlows.item(i);
            model.getFlows().add(new MessageFlow(
                    el.getAttribute("id"),
                    emptyToNull(el.getAttribute("name")),
                    el.getAttribute("sourceRef"),
                    el.getAttribute("targetRef")
            ));
        }
    }

    private void attachLaneIds(ProcessModel model) {
        // Build map nodeId -> laneId
        Map<String, String> nodeToLane = new HashMap<>();
        for (Lane lane : model.getLanes()) {
            for (String ref : lane.getNodeRefs()) {
                nodeToLane.put(ref, lane.getId());
            }
        }

        for (Node n : model.getNodes()) {
            String laneId = nodeToLane.get(n.getId());
            if (laneId != null) n.setLaneId(laneId);
        }
    }

    /* -------------------- mapping helpers -------------------- */

    private ActivityType mapActivityType(String localName) {
        if (localName == null) return ActivityType.UNKNOWN;
        return switch (localName) {
            case "task" -> ActivityType.TASK;
            case "userTask" -> ActivityType.USER_TASK;
            case "sendTask" -> ActivityType.SEND_TASK;
            case "receiveTask" -> ActivityType.RECEIVE_TASK;
            case "serviceTask" -> ActivityType.SERVICE_TASK;
            case "scriptTask" -> ActivityType.SCRIPT_TASK;
            case "manualTask" -> ActivityType.MANUAL_TASK;
            case "businessRuleTask" -> ActivityType.BUSINESS_RULE_TASK;
            case "callActivity" -> ActivityType.CALL_ACTIVITY;
            case "subProcess" -> ActivityType.SUB_PROCESS;
            default -> ActivityType.UNKNOWN;
        };
    }

    private GatewayType mapGatewayType(String localName) {
        if (localName == null) return GatewayType.UNKNOWN;
        return switch (localName) {
            case "exclusiveGateway" -> GatewayType.EXCLUSIVE;
            case "parallelGateway" -> GatewayType.PARALLEL;
            case "inclusiveGateway" -> GatewayType.INCLUSIVE;
            case "eventBasedGateway" -> GatewayType.EVENT_BASED;
            case "complexGateway" -> GatewayType.COMPLEX;
            default -> GatewayType.UNKNOWN;
        };
    }

    private EventType mapEventType(String localName) {
        if (localName == null) return EventType.UNKNOWN;
        return switch (localName) {
            case "startEvent" -> EventType.START;
            case "endEvent" -> EventType.END;
            case "intermediateCatchEvent", "intermediateThrowEvent" -> EventType.INTERMEDIATE;
            default -> EventType.UNKNOWN;
        };
    }

    /* -------------------- XPath + utils -------------------- */

    private XPath newXPath() {
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix == null) return XMLConstants.NULL_NS_URI;
                return switch (prefix) {
                    case "bpmn" -> BPMN_NS;
                    default -> XMLConstants.NULL_NS_URI;
                };
            }

            @Override public String getPrefix(String namespaceURI) { return null; }
            @Override public Iterator<String> getPrefixes(String namespaceURI) { return Collections.emptyIterator(); }
        });
        return xp;
    }

    private String readOptionalAttr(XPath xp, Document doc, String expression) {
        try {
            String s = (String) xp.evaluate(expression, doc, XPathConstants.STRING);
            return emptyToNull(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
