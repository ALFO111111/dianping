package com.alfo.common.utils;


import com.alfo.common.domain.dto.UserDTO;

public class UserDTOHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUserDTO(UserDTO UserDTO){
        tl.set(UserDTO);
    }

    public static UserDTO getUserDTO(){
        return tl.get();
    }

    public static void removeUserDTO(){
        tl.remove();
    }
}
