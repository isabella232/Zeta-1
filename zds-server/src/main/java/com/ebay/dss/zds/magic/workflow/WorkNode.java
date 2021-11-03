package com.ebay.dss.zds.magic.workflow;

import com.ebay.dss.zds.magic.exception.UnWorkableNodeException;

import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Created by tatian on 2021/4/15.
 */
public class WorkNode<E> {

  @NotNull
  private E e;
  private WorkNode<E> next;
  private WorkNode<E> last;
  private String error;

  public WorkNode(@NotNull E e) {
    this.e = e;
  }

  public WorkNode<E> getNext() {
    return next;
  }

  public void setNext(WorkNode<E> next) {
    this.next = next;
  }

  public WorkNode<E> getLast() {
    return last;
  }

  public void setLast(WorkNode<E> last) {
    this.last = last;
  }

  public E requireWorkable() throws UnWorkableNodeException {
    if (error != null) throw new UnWorkableNodeException(error);
    return this.e;
  }

  public void setError(String error) {
    this.error = error;
  }


}
