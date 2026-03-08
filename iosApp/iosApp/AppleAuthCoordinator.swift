//
//  AppleAuthCoordinator.swift
//  iosApp
//
//  Created by Alberto Medina on 8/3/26.
//
import Foundation
import AuthenticationServices
import CryptoKit
import ComposeApp

class AppleAuthCoordinator: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    
    private var currentNonce: String?
    private var onSuccess: ((String, String) -> Void)?
    private var onError: ((String) -> Void)?
    
    func startSignIn(onSuccess: @escaping (String, String) -> Void, onError: @escaping (String) -> Void) {
        self.onSuccess = onSuccess
        self.onError = onError
        
        let nonce = randomNonceString()
        currentNonce = nonce
        let appleIDProvider = ASAuthorizationAppleIDProvider()
        let request = appleIDProvider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
        
        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        authorizationController.performRequests()
    }
    
    // MARK: - ASAuthorizationControllerDelegate
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
            guard let nonce = currentNonce else {
                onError?("Estado inválido: No se encontró el nonce.")
                return
            }
            guard let appleIDToken = appleIDCredential.identityToken else {
                onError?("No se pudo obtener el identity token.")
                return
            }
            guard let idTokenString = String(data: appleIDToken, encoding: .utf8) else {
                onError?("No se pudo serializar el token string.")
                return
            }
            
            // ¡Éxito! Devolvemos el Token y el Nonce a Kotlin
            onSuccess?(idTokenString, nonce)
        }
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        onError?("Error al iniciar sesión con Apple: \(error.localizedDescription)")
    }
    
    // MARK: - Presentation Context
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Busca la ventana activa para mostrar el diálogo de Apple
        return UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? UIWindow()
    }
    
    // MARK: - Helpers de Criptografía obligatorios por Apple
    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remainingLength = length
        
        while remainingLength > 0 {
            let randoms: [UInt8] = (0 ..< 16).map { _ in
                var random: UInt8 = 0
                let errorCode = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                if errorCode != errSecSuccess { fatalError("No se pudo generar un número aleatorio") }
                return random
            }
            randoms.forEach { random in
                if remainingLength == 0 { return }
                if random < charset.count {
                    result.append(charset[Int(random)])
                    remainingLength -= 1
                }
            }
        }
        return result
    }
    
    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        let hashString = hashedData.compactMap { String(format: "%02x", $0) }.joined()
        return hashString
    }
}
