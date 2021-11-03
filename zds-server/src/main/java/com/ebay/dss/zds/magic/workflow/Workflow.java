package com.ebay.dss.zds.magic.workflow;

import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tatian on 2021/4/15.
 */
public class Workflow<E> implements Iterable<WorkNode<E>> {

  private List<E> elements;
  private List<WorkNode<E>> workNodes;

  private Workflow() {

  }

  public List<E> asRawTypes() {
    return elements;
  }

  public List<WorkNode<E>> workNodes() {
    return this.workNodes;
  }

  public WorkNode<E> last() {
    return (this.workNodes == null || this.workNodes.size() == 0)? null : this.workNodes.get(this.workNodes.size() - 1);
  }

  @Override
  public Iterator<WorkNode<E>> iterator() {
    return workNodes.iterator();
  }

  public static class WorkflowBuilder<E> {

    private Workflow<E> workflow = new Workflow<>();

    public WorkflowBuilder() {
      this.workflow.workNodes = new LinkedList<>();
      this.workflow.elements = new LinkedList<>();
    }

    public WorkflowBuilder(List<E> elements) {
      this.workflow.elements = elements;
      this.workflow.elements.forEach(this::addRaw);
    }

    public Workflow<E> build() {
      return this.workflow;
    }

    public WorkNode<E> addRaw(E e) {
      this.workflow.elements.add(e);
      WorkNode<E> workNode = new WorkNode<>(e);
      WorkNode<E> last = workflow.last();
      if (last != null) {
        last.setNext(workNode);
        workNode.setLast(last);
      }
      this.workflow.workNodes.add(workNode);
      return workNode;
    }

    public WorkNode<E> addError(E e, String error) {
      WorkNode<E> failedNode = addRaw(e);
      failedNode.setError(error);
      return failedNode;
    }

    public void addRawElements(List<E> elements) {
      this.workflow.elements.addAll(elements);
      this.workflow.elements.forEach(this::addRaw);
    }

  }

}
