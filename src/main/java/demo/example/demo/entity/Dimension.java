package demo.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Dimension")
public class Dimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer width;
    private Integer height;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}
