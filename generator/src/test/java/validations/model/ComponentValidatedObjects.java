package validations.model;

import java.util.Objects;
import javax.validation.constraints.Pattern;

public class ComponentValidatedObjects {
  @Pattern(
      regexp = "^\\d+$"
  )
  private String name;

  public ComponentValidatedObjects name(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (other == null || getClass() != other.getClass()) return false;
    ComponentValidatedObjects o = (ComponentValidatedObjects) other;
    return Objects.equals(name, o.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(", name=").append(name);
    return builder.replace(0, 2, "ComponentValidatedObjects{").append('}').toString();
  }
}
