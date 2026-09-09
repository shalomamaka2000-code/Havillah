package havillah_backend.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CategoryResponse {

    private Long id;
    private String name;
    private String description;

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

}
