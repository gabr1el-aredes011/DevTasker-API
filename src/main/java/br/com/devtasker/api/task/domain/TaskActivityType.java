package br.com.devtasker.api.task.domain;

public enum TaskActivityType {
    TASK_CREATED,
    TASK_UPDATED,
    TASK_MOVED,
    TASK_ARCHIVED,
    CHECKLIST_ITEM_ADDED,
    CHECKLIST_ITEM_UPDATED,
    CHECKLIST_ITEM_REMOVED,
    COMMENT_ADDED,
    COMMENT_EDITED,
    COMMENT_REMOVED
}
