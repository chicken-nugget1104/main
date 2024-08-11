package com.mojang.minecraft;

import java.io.*;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private static int userCount = 1;
    private String name;

    public User(String name) {
        this.name = name + getNextUserNumber();
    }

    public String getName() {
        return name;
    }

    public void serialize(DataOutputStream dos) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(dos);
        oos.writeObject(this);
    }

    public static User deserialize(DataInputStream dis) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(dis);
        return (User) ois.readObject();
    }

    private int getNextUserNumber() {
        return userCount++;
    }
}
