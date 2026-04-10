sealed class Screen(val route: String) {
    // ... other screens ...

    object Call : Screen("call/{sessionId}/{calleeName}/{isCaller}/{callerId}") {
        fun go(
            sessionId: String, 
            calleeName: String, 
            isCaller: Boolean, 
            callerId: String // Ensure this 4th argument exists
        ): String {
            return "call/$sessionId/$calleeName/$isCaller/$callerId"
        }
    }
}