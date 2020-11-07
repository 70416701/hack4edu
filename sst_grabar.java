package org.evalfin.forh.ui.sst_grabar;

import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.evalfin.forh.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class sst_grabar extends Fragment implements Response.Listener<JSONObject>, Response.ErrorListener{
    private static final int REQ_CODE_SPEECH_INPUT=100;
    TextView mEntradaVoz,txv_respuesta_por,txv_respuesta_si_no;
    ImageButton mBotonHablar,btn_llamar_contac;

    EditText et_sms_destino, et_sms_cuerpo;
    Button enviar_sms, llamar;
    Chronometer chronometer;
    boolean iniciar = false;
    long detenerce;
    List<String> jsonResponses;

    ProgressDialog progreso;
    RequestQueue request;
    JsonObjectRequest jsonObjectRequest;
    ArrayList<String> result;
    private SstGrabarViewModel mViewModel;
    private OnFragmentInteractionListener mListener;
    public sst_grabar(){

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.sst_grabar_fragment, container, false);
        mEntradaVoz=vista.findViewById(R.id.textoEntrada);
        mBotonHablar=vista.findViewById(R.id.btnhablar);
        chronometer = vista.findViewById(R.id.chr_contador);
        txv_respuesta_por = vista.findViewById(R.id.txv_respuesta_por);
        txv_respuesta_si_no = vista.findViewById(R.id.txv_respuesta_si_no);
        btn_llamar_contac=vista.findViewById(R.id.btn_llamar_contac);

        request = Volley.newRequestQueue(getContext());
        mBotonHablar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                iniciarEntradaVoz();
            }
        });

        btn_llamar_contac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog sms = new Dialog(getContext());
                sms.setTitle("SMS");
                sms.setCancelable(true);
                sms.setContentView(R.layout.llamada_sms);
                sms.show();

                et_sms_destino = sms.findViewById(R.id.et_sms_destino);
                et_sms_cuerpo = sms.findViewById(R.id.et_sms_cuerpo);
                enviar_sms = sms.findViewById(R.id.btnEnviarSMS);

                enviar_sms.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String sms_destino = et_sms_destino.getText().toString();
                        String sms_cuerpo = et_sms_cuerpo.getText().toString();

                        Uri uri = Uri.parse("smsto:"+sms_destino);
                        Intent sms = new Intent(Intent.ACTION_SENDTO,uri);
                        sms.putExtra("sms_body",sms_cuerpo);
                        startActivity(sms);
                    }
                });

                llamar = sms.findViewById(R.id.btnLlamar);
                llamar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String numero_llamada = et_sms_destino.getText().toString();

                        Uri numero = Uri.parse("tel:" + numero_llamada);
                        Intent hacerllamada = new Intent(Intent.ACTION_DIAL, numero);
                        startActivity(hacerllamada);
                    }
                });
            }
        });

        return vista;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SstGrabarViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
        case REQ_CODE_SPEECH_INPUT:{
            iniciarCronometro();
            if (resultCode==Activity.RESULT_OK && data!=null){
                result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mEntradaVoz.setText(result.get(0));
                //ejecutarServicio("http://192.168.1.7:8080/sample1/wsJSONRegistroGrab.php");
                //ejecutarServicio("https://painless-app.herokuapp.com/aggresion");
                cargarServicio("https://painless-app.herokuapp.com/aggresion");
                pararCronometro();
            }
            break;
        }
    }
}

    private void iniciarEntradaVoz(){
        Intent intent= new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hola habla ahora");
        try {

            startActivityForResult(intent,REQ_CODE_SPEECH_INPUT);
            iniciarCronometro();
        }
        catch (ActivityNotFoundException e){
            Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
        }
    }

    public void cargarServicio(String postUrl){
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        JSONObject postData = new JSONObject();
        try {
            postData.put("sentence", mEntradaVoz.getText().toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                jsonResponses = new ArrayList<>();

                try {
                    JSONArray jsonArray = response.getJSONArray("message");

                    for(int i = 0; i < jsonArray.length(); i++){
                        String str = jsonArray.getString(i);
                        jsonResponses.add(str);
                    }
                    txv_respuesta_si_no.setText("Se considera acoso:"+jsonResponses.get(0).toString());
                    txv_respuesta_por.setText("% de acoso: "+jsonResponses.get(1).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);

    }

    private void pararCronometro() {
        if(iniciar) {
            chronometer.stop();
            detenerce = SystemClock.elapsedRealtime() - chronometer.getBase();
            iniciar = false;
            detenerce = 0;
        }
    }

    private void iniciarCronometro() {
        if(!iniciar) {
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            iniciar = true;
        }
    }

    /*public void ejecutarServicio(String URL) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(getContext(), "retorno: "+response.toString(), Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> parametros = new HashMap<String,String>();
                //parametros.put("idUsuario","3");
                parametros.put("sentence",mEntradaVoz.getText().toString());

                return parametros;
            }
        };
        request.add(stringRequest);
    }*/


    @Override
    public void onErrorResponse(VolleyError error) {
        progreso.hide();
        Toast.makeText(getContext(), "no se pudo registrar"+error.toString(), Toast.LENGTH_SHORT).show();
        Log.i("Error", error.toString());
    }

    @Override
    public void onResponse(JSONObject response) {
        progreso.hide();
        Toast.makeText(getContext(), "se ha registrado exitosamente", Toast.LENGTH_SHORT).show();
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) mListener.onFragmentInteraction(uri);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction (Uri uri);
    }
}
