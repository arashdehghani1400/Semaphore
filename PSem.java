import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class PSem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int numberClient = scanner.nextInt();

        ClientsQueue outsideClients = new ClientsQueue();
        ClientsQueue insideClients = new ClientsQueue();
        Room room = new Room();
        Officer officer = new Officer(outsideClients, insideClients, room);
        ArrayList<Client> clients = new ArrayList<>();

        for (int i = 1; i <= numberClient; i++) {
            Client client = new Client(i, outsideClients, insideClients, room);
            outsideClients.queue.add(client);
            clients.add(client);
        }

        officer.start();
        for (Client client : clients) {
            client.start();
        }
    }
}

enum ClientState {
    OUTSIDE,
    INSIDE,
    IN_ROOM
}

class Client extends Thread {
    int number;
    Message message;
    ClientsQueue outsideClients;
    ClientsQueue insideClients;
    Room room;
    ClientState state = ClientState.OUTSIDE;


    public Client(int number, ClientsQueue outsideClients, ClientsQueue insideClients, Room room) {
        this.number = number;
        this.outsideClients = outsideClients;
        this.insideClients = insideClients;
        this.room = room;
        message = new Message(String.valueOf(number));
    }

    void acquireSem() {
        try {
            room.semaphore.acquire();
            outsideClients.semaphore.acquire();
            insideClients.semaphore.acquire();
            sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void releaseSem() {
        room.semaphore.release();
        outsideClients.semaphore.release();
        insideClients.semaphore.release();
        try {
            sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        while (!outsideClients.queue.isEmpty() || !insideClients.queue.isEmpty() || room.client != null) {
            acquireSem();
            if (state == ClientState.OUTSIDE) {
                message.waitingForInterview();
            }
            releaseSem();
            acquireSem();
            if (state == ClientState.OUTSIDE && outsideClients.queue.peek().number == number && insideClients.queue.size() < 3) {
                insideClients.queue.add(outsideClients.queue.remove());
                state = ClientState.INSIDE;
                message.inHall();
            }
            releaseSem();
            acquireSem();
            if (state == ClientState.INSIDE && insideClients.queue.peek().number == number && room.client == null) {
                message.enterRoom();
                state = ClientState.IN_ROOM;
                room.client = insideClients.queue.remove();
            }
            releaseSem();
            acquireSem();
            if (state == ClientState.IN_ROOM) {
                message.exitRoom();
                room.client = null;
            }
            releaseSem();
        }
    }
}

class ClientsQueue {
    Queue<Client> queue = new LinkedList<>();
    Semaphore semaphore = new Semaphore(1);
}

class Room {
    Client client;
    Semaphore semaphore = new Semaphore(1);
}

class Officer extends Thread {
    Message message = new Message("Officer");
    ClientsQueue outsideClients;
    ClientsQueue insideClients;
    Room room;


    public Officer(ClientsQueue outsideClients, ClientsQueue insideClients, Room room) {
        this.outsideClients = outsideClients;
        this.insideClients = insideClients;
        this.room = room;
    }

    void acquireSem() {
        try {
            room.semaphore.acquire();
            outsideClients.semaphore.acquire();
            insideClients.semaphore.acquire();
            sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void releaseSem() {
        room.semaphore.release();
        outsideClients.semaphore.release();
        insideClients.semaphore.release();
        try {
            sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        while (!outsideClients.queue.isEmpty() || !insideClients.queue.isEmpty() || room.client != null) {
            acquireSem();
            if (room.client == null && insideClients.queue.isEmpty()) {
                message.sleepMrX();
            }
            releaseSem();
        }
    }
}


class Message {
    String nameThread;

    Semaphore semaphore = new Semaphore(1);

    public Message(String nameThread) {
        this.nameThread = nameThread;
    }

    public void waitingForInterview() {
        System.out.println(nameThread + " waiting for interview outside of company");
    }

    public void inHall() {
        System.out.println(nameThread + " in hall");
    }

    public void enterRoom() {
        System.out.println(nameThread + " enter room");
    }

    public void exitRoom() {
        System.out.println(nameThread + " exit room");
    }

    public void sleepMrX() {
        System.out.println("Mr X is sleeping");
    }
}

