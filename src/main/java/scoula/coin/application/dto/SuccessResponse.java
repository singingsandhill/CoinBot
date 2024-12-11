package scoula.coin.application.dto;

public class SuccessResponse {
    private final Boolean success;
    private final String response;

    public SuccessResponse(Object data) {
        this.success = true;
        this.response = new DataDTO(data).toString();
    }


}
