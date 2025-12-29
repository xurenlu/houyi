package com.ruoran.houyi;

import com.tencent.wework.RSAEncrypt;
import org.junit.jupiter.api.Test;

/**
 * @author xurenlu
 * created at 2024/2/2-10:04
 */
public class DecryptTest {
    @Test
    public void testDecrypt() throws Exception {
        String randomKey = "+rqVBxkZtR0JfVGPJPz3VaUQ7yda7n+d1B4tFfbjlSLLKJ4xGjsbGWPP1NYgQB90rI0Ihw6IcH9yIl5MUWgomnNVxSAqqYlO0HF83/+YXS49M6j62Zbc3rYtG5oVRGZGnWO6h4zBu5qGlHG562h7CqB1adAh0jllsILKbqZxhTrfNt8FzXIF4DAECK8hc//CyxmJpCzxQ+UeRH9GGhjAe5budJaVRBTM+mWnd/6nKIizFNn3KV+XJQo1DHA6a3+gCUiUqzSF8UjT5lVxh3FUFoFBAiDHFypfQyWNppsQ==";
        String randomKey2 = "bXdjbA1adqrIbcIX5EuVq4kfcXiv92P64WAMXYvE/ceqOK7frUs2Y6mukUEcu3Mdrs84U13fWr6wbbtn/X3Kaa6u8cxlk+tWWpujp1F7IR474rnq8mulaP2cWECWl3PcK0PohKPr0Gpyfy+iS/h7gzLy/JYXbTiocGzV217NfVNvbbTvpcUMMOL8S8gJJHyyXQrOmeUKSyk/ZKYPZ2ggfWFno69p9mFDpUJreTva/uvz2/eAw3I1T/RaJU5qblQ6vn4Oz71j6DBxdb2UvoZctKj9aroJ/IjwtyxynQAWCCaHYQeRMgads9TrlkS3vDGl0rMEePvCv9EUpcA/3r9wgQ==";
        String chatData = "YB4adAn0K+SfYmBQTpPTOom4wcMBIQlAT6SAYxdw1/73oa8bsc0Bi1eK4FmB6h9NLyfogMbb30Oh5UWAliFl0iEQAeVpzHh63kHXXf7NKrTX482Jj9WtMxhNrCYVGC6UUmv4rkhEwwQoe4baULHy047Z/h9RggeE2Qh5gFu5Osyn1grPQT0QXixvWf80XGMYZIvfkzbKb7Do9VMfUV8zkwbocrlwAxD11mmYS8iUcNw4qk1bdy4Zqcq0kLiFoWxS6zJJ0M2eAY7RZsrI83/k+8s6QajeaCkt69fHT7BOp9UDiYBc/ShiiLmt8hOUdaNbaZ4AX3iB5Ph59GVP3mGWT8AbPG9utwqttGG7Ye3SagIAu6qG0/jlI430qa";
        String key = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCkS9olKVAiKdsT\n" +
                "DAMqB5amiF+WzZypgKnI9v4G7g2FDSFhjjCPeiZGOJe3G2/Kjd9wDTBDlDQPmJf5\n" +
                "WDtqQwyFIrYHnILb9g2vIMUcRxpnP6BBfZvlF4LbbJJNNrznoR5X06W5IZcCb7Sf\n" +
                "b1Re2Gs508/Pj0im4qtspGvk/JAPM1XNZQ3UJR94Vc2miKMMhs+CiR3r2OmSuGAC\n" +
                "pcVhcrfKeEdJlKExT+7gtok/5UVsytaH76UKVofCKiuS5n6ZttFZyod3AIh8Lmgs\n" +
                "tA45Tx2CxHhy39WzP0tCG1pCTDyhVGIRxGxbxGxtOxw5HtZf3VmY/XrCwrYv2LOM\n" +
                "KdV4wC7NAgMBAAECggEAapJlU3uj5jU+TPGaz9WSTD3ju39uIqk5+Qj2KGqcTHUC\n" +
                "9TP1nev/DcfB6d2bO0mh6R4083EcAA3cbrpj9/68DVRBvVgxrhrCL5pTIY+hud2U\n" +
                "x0aCLC1/DXQ0xm8+RAXIF/JK6na2bLcm89CUat15WE5j22A3IUGhLtzMvAF0NNbu\n" +
                "vW3w5EvRCXqdhQGHrGynDh65+Mjn7pjJGGJFu9C61uvsT8Dw0tvZUtVY4aZIUl6s\n" +
                "K+YMoY4N5yWdF54ToUW44M8C5lSReckGISeh2pIuS/SivU3WjjdIenD9rYlcg4+v\n" +
                "d56uPmUOG9Mvw3zsSjQRhqa06Gd748fw/GbY9MOrxQKBgQDN8u0F6SHJTg5zRkPt\n" +
                "yThKGMIP2ayPzRJ0nuU28VSjRd+l7Db8NPqMjilBE8pyMqI7x86ryzk+VfxVhS9M\n" +
                "iNkhtKrCzX1l/51rT9q/Yk2+QPlyr5YWljNzlUWMdjfDp8EBRy5s9ax7bghIxjQC\n" +
                "5hEXAoRw05swofBFolqgG6+IFwKBgQDMOYP7jCaTPPqiFPmzSC67+sm1yStszGgO\n" +
                "wXOqy3354PyZg5XySO9aTA5garCtDlY2Lh5nsqjr5/tMXzKoEgCCoKmYRavSADWr\n" +
                "zMP/BHWAJe+SWiWiNSpMJEcz8dfwfMZxuSnZkwif3p4un50/Ec7vfHySAYf0rRjD\n" +
                "WdljsuwquwKBgE5V+JIm+xL2/cgbEfuAbkjA4g/lXB3Wgw6Y4dbkNK1mBou2LbQA\n" +
                "8sxDxq1aOcb2O39BoPr3ce6kBjcBUdxqsSyA6/Ls9qW1dMs8tJfXbHrRfBCDeTNe\n" +
                "LnPoc2vaC5wAUwmJab9IMzM/xybnPSFdIWL/MiE4W+9DqYDTIGWGIl5BAoGAceVp\n" +
                "zDJ/mQIbLaXRFMGe/suBD1cgVs8xAXm2Tnyqx19IwTz3tWYjxxI/uloGKp2iSxkx\n" +
                "b9feCaMZLaCyGbXgfvmnW/sPNlFTnXrXnDQaa9u8XrzT3EEWU9yvsTKhoceUzPvd\n" +
                "cTBlUPPQ+GOgSPpflISy3KLp9fVLqDdF3Cp/N90CgYAwoVbY//oDN3FjxsKq9MW0\n" +
                "/bo4vApkZmekPwQ2gZqBs3dGapZ/2WEkzjscr+r+corQm3N8+1ErBMCoajEC95r+\n" +
                "DUdmMcJ5F+Tmwpl87mcAqZ/g1LgkYiVVngiYu7PaZb/U/Pzf1TSckXqEIGLtzFJB\n" +
                "E5iYgI2rsSEuW1tfma9gig==\n" +
                "-----END PRIVATE KEY-----";
        //----BEGIN PRIVATE KEY-----\\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDGJwCK9UiUf9OQ\\nYc4ib1dd2lobcmdEhhyNnUXxi39LS9IAUyAwGccQdptGhY12w0w6JZ6UCZxYucuG\\nwlvqNSufUxxk4VbFAli3F4hlRL7zLKXsgiRKAmvqNhupIusXez3Dnpnp1IeMJvNp\\n7HANgeNA+9sbyIgM/93+1o1lXuUJxXfs05T/0wGk4nA74bfH9VkpN4+xkjs82ova\\nHBljYj5oAUy2p+i/8Afp4YqAfOAWSpW3GBYWtTFMEOr4EDeNlK23xzkiWeKNXhaQ\\nzoQwudGd9AxtTe6/zPeHMluwWrMrfvQfRhuzpWb8SoPANJN5KG0NPjKr/0lL0HDM\\nAFUoOPXHAgMBAAECggEACo1VKBE71jygZZXvxkGro2HXXpLPEQUbTCdN7DkDhFz7\\nR+0lT6pvwV6vkifWiraHvqj2Muf4HXl3byu2fVhrhEaGwpCFcGRZmRZ1uv7YNFby\\nVw/vIsnSOVy8SFuFrXBwr5Qomh0pQrtoRwHCTdXW5kUDPA2HwTl29vUx/m35QHXd\\nmcRYE6diiT6gJIENBLiWiUyFEPy7d5KZPp/OhFvnlbEDZ6kQoNWLlNeArZc7Fpw2\\np0v7Md5mFTMQK8U0tywxJE/KMNqU5qb/7L/w7WJCV/q8AzmXwGXrXI40cdussPFB\\nvHmDSA+ow8dGRSEOpT4M1dvh2ffi3Khi+u0Yzyuh4QKBgQDjnNVN+v0S1kBQrtpJ\\nuh60vJHsHeiLc4E/Q6F2RssKDTfj8S8JJUKzK11WsbvXVu8i6HvV+lE2V+ArzXOP\\nk9h4XEIGo/4E3xSSHe8w5oEBwnkQvQ048PzPV/m6jhyzMyvmLam8/Q3Dlc/9Wv4r\\nQlcmx5z8yHrglfYjwxYUoEiCjwKBgQDe3ZFJg1Wy58gEnvx2SUhknbc3fdGOm92R\\nJ+7j8W2w78uHh6zJKm9kHqzf9X2lli8ylhUZZE5Fgmj7G4wK+Tp/jaAB4xajpFGL\\nNyxefTAtJOaEtryaqwqQKdMu2JZsMWV9T/9EVm6AFe9MatYvatjAML64LHLj5w3S\\nJYRPxMoVSQKBgQCuS2WHzl/G8evUHOEpPe3PhurS/WAakTtyv44/rRxDuTWIRiM3\\nhKHb46QZBAcMchSzDYXuqD1SON5/jFcmphdIq5Uf0qps/oqZDXUhZBF70Hi2mZ4r\\ncVaWTA7+jV5q1w+RtnvZLYpBsFHR98DUTXvBRW/wnnCB1DPk1Lnu14JO/wKBgQCu\\nD1x2jtWy6eXE/irtc2TP+IHtHB48BC3zPb4NVzU1mMNcMbHYV4UyK+cW75PXbMml\\n96O49idPkH/Phv9EXsy6bHFIqAS5gA+T7F6B2cJzr7s+cb4yCl4wpAnlL5GkJJxA\\nV+dlhx+8d+UyyMpJtGhfCnMaQtTkfEPGGjkRWAAAyQKBgQCXeRvm3FrrzBrkpURv\\nyFwoXX9EoOS+S0guJrh6iA9Pc4HG/aMHkS2y62npyGLgzn+uolTHSnAUmlbBhPZ9\\ndhnogc7+CjYriM+KVLS0e/3wTSzKpkWIF+FtO1AuvupZH/njLPFcQhO0gTX0qEo4\\n5zM8E2Je2tpVRGqStYJSM7nUog==\\n-----END PRIVATE KEY-----\n";
        Message msg = new Message();
        msg.setMode(2);
        RSAEncrypt.decryptByPriKey(randomKey2, key);
        //RSAEncrypt.decryptByPriKey(randomKey, key);

        //String text = msg.decryptData(randomKey, chatData);
        //System.out.println(text);
    }
}
