import React from 'react';
import PropTypes from 'prop-types';

/**
 * Card component to display a list of contas (accounts payable).
 * Recebe um título, a lista de contas e um ícone opcional.
 */
export const ContasCard = ({ title, contas, icon }) => (
  <div className="p-4 bg-white rounded-xl shadow-sm border">
    <div className="flex items-center mb-2">
      {icon && React.cloneElement(icon, { className: `${icon.props.className || ''} mr-2` })}
      <h3 className="font-semibold text-slate-800">{title}</h3>
    </div>
    {contas.length === 0 ? (
      <p className="text-slate-400 text-sm">Nenhuma conta.</p>
    ) : (
      <ul className="list-disc list-inside space-y-1 text-sm">
        {contas.map((c) => (
          <li key={c.id} className="break-all">
            <span className="font-medium">{c.descricao}</span> –
            <span className="ml-1 text-emerald-600">
              R$ {Number(c.valor).toFixed(2)}
            </span>
            <span className="ml-2 text-slate-500">
              {new Date(c.vencimento).toLocaleDateString('pt-BR')}
            </span>
          </li>
        ))}
      </ul>
    )}
  </div>
);

ContasCard.propTypes = {
  title: PropTypes.string.isRequired,
  contas: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number,
      descricao: PropTypes.string,
      valor: PropTypes.number,
      vencimento: PropTypes.string,
    })
  ),
  icon: PropTypes.element,
};

ContasCard.defaultProps = {
  contas: [],
  icon: null,
};
