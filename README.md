[![](https://jitpack.io/v/ebayedq/swing-beansbinding.svg)](https://jitpack.io/#ebayedq/swing-beansbinding)

# Beans Binding (JSR 295)

This is a fork of the [JSR-295](https://jcp.org/en/jsr/detail?id=295) reference implementation.

This version (which I called 1.3.0) includes two notable differences to the latest release (1.2.1) you can find on maven central:

### 1. Performance improvements

After the 1.2.1 release, a significant [performance improvement](https://github.com/ebayedq/swing-beansbinding/commit/5f188348abb2679809001b9e7429ed722574e940) was added (see [here](https://blog.marcnuri.com/beansbinding-performance-issue-37) for details) that was never released.

### 2. Conditional JTable cell enablement

Previously you would bind your JTable like so:

```
List<Person> persons = createList();
JTableBinding<Person, List<Person>, JTable> tableBinding = SwingBindings.createJTableBinding(READ_WRITE, persons, table);

tableBinding.addColumnBinding(BeanProperty.create("firstName"))
    .setColumnName("First name")
    .setEditable(true); //or false

tableBinding.bind();
```

That means, that you could only make entire columns editable or not editable. Now it is possible to define this per row element using a Predicate:

```
tableBinding.addColumnBinding(BeanProperty.create("firstName"))
    .setColumnName("First name")
    .setEditableWhen(Person::isMale);
```
