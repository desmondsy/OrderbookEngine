package MatchingEngine;

public class UnexpectedRemainingVolumeException extends RuntimeException {

    public UnexpectedRemainingVolumeException(){
        super();
    }

    public UnexpectedRemainingVolumeException(String message){
        super(message);
    }
}
