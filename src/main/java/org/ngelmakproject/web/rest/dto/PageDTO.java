package org.ngelmakproject.web.rest.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

public class PageDTO<T> {
  private List<T> content; // The actual page content (list of elements)
  private Integer number; // Current page number
  private Integer size; // Number of elements per page
  private Long totalElements; // Total number of elements across all pages
  private Integer totalPages; // Total number of pages
  private Boolean isLast; // Is this the last page
  private Boolean isFirst; // Is this the first page
  private Boolean hasNext; // Is there a next page
  private Boolean hasPrevious; // Is there a previous page
  private List<SortDTO> sorts; // Sorting information

  public PageDTO(Page<T> page) {
    content = page.getContent();
    number = page.getNumber();
    size = page.getSize();
    totalElements = page.getTotalElements();
    totalPages = page.getTotalPages();
    isFirst = page.isFirst();
    isLast = page.isLast();
    hasNext = page.hasNext();
    hasPrevious = page.hasPrevious();
    sorts = page.getSort().stream()
        .map(order -> new SortDTO(order.getProperty(), order.getDirection().name()))
        .collect(Collectors.toList());
  }

  public PageDTO(Slice<T> page) {
    content = page.getContent();
    number = page.getNumber();
    size = page.getSize();
    isFirst = null;
    isLast = null;
    hasNext = page.getSize() == content.size();
    hasPrevious = null;
    sorts = page.getSort().stream()
        .map(order -> new SortDTO(order.getProperty(), order.getDirection().name()))
        .collect(Collectors.toList());
  }

  public List<T> getContent() {
    return content;
  }

  public void setContent(List<T> content) {
    this.content = content;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public long getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(long totalElements) {
    this.totalElements = totalElements;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }

  public boolean isLast() {
    return isLast;
  }

  public void setLast(boolean isLast) {
    this.isLast = isLast;
  }

  public boolean isFirst() {
    return isFirst;
  }

  public void setFirst(boolean isFirst) {
    this.isFirst = isFirst;
  }

  public boolean isHasNext() {
    return hasNext;
  }

  public void setHasNext(boolean hasNext) {
    this.hasNext = hasNext;
  }

  public boolean isHasPrevious() {
    return hasPrevious;
  }

  public void setHasPrevious(boolean hasPrevious) {
    this.hasPrevious = hasPrevious;
  }

  public List<SortDTO> getSorts() {
    return sorts;
  }

  public void setSorts(List<SortDTO> sorts) {
    this.sorts = sorts;
  }

  @Override
  public String toString() {
    return "PageDTO [number=" + number + ", size=" + size + ", totalElements=" + totalElements
        + ", totalPages=" + totalPages + ", isLast=" + isLast + ", isFirst=" + isFirst + ", hasNext=" + hasNext
        + ", hasPrevious=" + hasPrevious + ", sorts=" + sorts + "]";
  }
}
